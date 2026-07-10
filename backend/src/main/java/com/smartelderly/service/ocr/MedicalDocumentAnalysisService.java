package com.smartelderly.service.ocr;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.DocumentClassItem;
import com.smartelderly.api.dto.MedicalOcrView;

/**
 * 并行「文件检测分类 + 高精度全文 OCR」，再路由结构化医疗接口（可选）：优先根据 OCR 全文关键词命中，
 * 否则回退到百度云 doc_classify 主类别。
 */
@Service
public class MedicalDocumentAnalysisService {

    private final BaiduAccurateBasicOcrService accurateBasicOcrService;
    private final BaiduDocClassifyService docClassifyService;
    private final BaiduMedicalStructuredOcrService structuredOcrService;
    private final ImagePreprocessingService preprocessingService;

    private static final Logger log = LoggerFactory.getLogger(MedicalDocumentAnalysisService.class);

    public MedicalDocumentAnalysisService(
            ImagePreprocessingService preprocessingService,
            BaiduAccurateBasicOcrService accurateBasicOcrService,
            BaiduDocClassifyService docClassifyService,
            BaiduMedicalStructuredOcrService structuredOcrService) {
        this.preprocessingService = preprocessingService;
        this.accurateBasicOcrService = accurateBasicOcrService;
        this.docClassifyService = docClassifyService;
        this.structuredOcrService = structuredOcrService;
    }

    public MedicalOcrView analyze(byte[] imageBytes) {
        long startNs = System.nanoTime();
        int inputBytes = imageBytes == null ? 0 : imageBytes.length;
        log.info("ocr.analyze.start inputBytes={}", inputBytes);

        byte[] processed = imageBytes;
        try {
            processed = preprocessingService.preprocess(imageBytes);
        } catch (Exception e) {
            // preprocess should not break the flow
            log.warn("image preprocess failed, using original image: {}", e.getMessage());
            processed = imageBytes;
        }

        final byte[] processedBytesForOcr = processed;

        int processedBytes = processedBytesForOcr == null ? 0 : processedBytesForOcr.length;
        log.info("ocr.analyze.preprocess inputBytes={} processedBytes={}", inputBytes, processedBytes);

        CompletableFuture<BaiduDocClassifyService.ClassifyResult> classifyFuture =
            CompletableFuture.supplyAsync(() -> docClassifyService.tryClassifyBytes(processedBytesForOcr));
        CompletableFuture<MedicalOcrView> ocrFuture =
            CompletableFuture.supplyAsync(() -> accurateBasicOcrService.recognizeBytes(processedBytesForOcr));

        MedicalOcrView ocr = unwrapJoin(ocrFuture);
        BaiduDocClassifyService.ClassifyResult cls = classifyFuture.join();

        List<DocumentClassItem> classes = cls.isEmpty() ? null : cls.items();
        Map<String, Object> classifyRaw =
                cls.isEmpty() || cls.raw().isEmpty() ? null : cls.raw();

        String routedSpecializedApi = null;
        Map<String, Object> specializedRaw = null;
        String structuredError = null;

        DocumentClassItem primary = pickPrimary(classes);
        Optional<String> conflict =
                MedicalClassificationConsistency.conflictHint(
                        primary != null ? primary.getType() : null, ocr.getFullText());

        Optional<SpecializedMedicalOcrApi> keywordRouted =
                MedicalTextKeywordRouter.resolve(ocr.getFullText());

        String structuredRouteSource = null;
        Optional<SpecializedMedicalOcrApi> routed = keywordRouted;
        if (routed.isPresent()) {
            structuredRouteSource = "keyword_text";
        } else if (conflict.isEmpty()
                && primary != null
                && primary.getType() != null) {
            routed = MedicalDocClassRouter.resolve(primary.getType());
            if (routed.isPresent()) {
                structuredRouteSource = "doc_classify";
            }
        }

        if (routed.isPresent()) {
            SpecializedMedicalOcrApi api = routed.get();
            routedSpecializedApi = api.getApiId();
            specializedRaw = structuredOcrService.tryRecognize(api, processedBytesForOcr).orElse(Map.of());
            if (specializedRaw.isEmpty()) {
                structuredError = switch (routedSpecializedApi) {
                    case "medical_report_detection" -> "检验报告结构化接口未返回结果，可能未开通、欠费或版式不匹配";
                    case "medical_prescription" -> "处方结构化接口未返回结果，可能未开通、欠费或版式不匹配";
                    case "medical_invoice", "medical_detail", "medical_statement" ->
                            "费用类结构化接口未返回结果，可能未开通、欠费或版式不匹配";
                    default -> "结构化接口未返回结果，可能未开通、欠费或版式不匹配";
                };
            }
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        int fullTextLength = ocr.getFullText() == null ? 0 : ocr.getFullText().length();
        int classCount = classes == null ? 0 : classes.size();
        int specializedSize = specializedRaw == null ? 0 : specializedRaw.size();
        log.info(
            "ocr.analyze.done elapsedMs={} inputBytes={} processedBytes={} fullTextChars={} classCount={} structuredApi={} structuredRouteSource={} specializedRawSize={} structuredError={}",
            elapsedMs,
            inputBytes,
            processedBytes,
            fullTextLength,
            classCount,
            routedSpecializedApi,
            structuredRouteSource,
            specializedSize,
            structuredError);

        return MedicalOcrView.builder()
                .fullText(ocr.getFullText())
                .raw(ocr.getRaw())
                .documentClasses(classes)
                .classifyRaw(classifyRaw)
                .routedSpecializedApi(routedSpecializedApi)
                .specializedRaw(specializedRaw)
                .structuredError(structuredError)
                .classificationWarning(conflict.orElse(null))
                .structuredRouteSource(structuredRouteSource)
                .build();
    }

    private static DocumentClassItem pickPrimary(List<DocumentClassItem> classes) {
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        return classes.stream()
                .max(Comparator.comparingDouble(MedicalDocumentAnalysisService::probabilityOrNegOne))
                .orElse(classes.get(0));
    }

    private static double probabilityOrNegOne(DocumentClassItem c) {
        return c.getProbability() != null ? c.getProbability() : -1.0;
    }

    private static <T> T unwrapJoin(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable c = e.getCause();
            if (c instanceof ApiException ae) {
                throw ae;
            }
            if (c instanceof RuntimeException re) {
                throw re;
            }
            throw new ApiException(5000, c != null ? c.getMessage() : "unknown error");
        }
    }
}
