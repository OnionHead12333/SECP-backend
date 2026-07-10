package com.smartelderly.service.medical;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.MedicalOcrView;
import com.smartelderly.api.dto.medical.ExtractedMedicalFieldsDto;
import com.smartelderly.api.dto.medical.MedicalSmartRecognitionResultDto;
import com.smartelderly.api.dto.medical.SuggestedCalendarEventDto;
import com.smartelderly.domain.MedicalDocument;
import com.smartelderly.domain.MedicalDocumentRepository;
import com.smartelderly.security.AuthPrincipal;
import com.smartelderly.service.ocr.MedicalDocumentAnalysisService;

@Service
public class MedicalSmartRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(MedicalSmartRecognitionService.class);

    private final MedicalAccessService medicalAccessService;
    private final MedicalDocumentAnalysisService medicalDocumentAnalysisService;
    private final MedicalDocumentStorageService medicalDocumentStorageService;
    private final MedicalFieldExtractionService medicalFieldExtractionService;
    private final MedicalFieldPostProcessor medicalFieldPostProcessor;
    private final MedicalDocumentRenderService medicalDocumentRenderService;
    private final MedicalDocumentRepository medicalDocumentRepository;
    private final ObjectMapper objectMapper;

    public MedicalSmartRecognitionService(
            MedicalAccessService medicalAccessService,
            MedicalDocumentAnalysisService medicalDocumentAnalysisService,
            MedicalDocumentStorageService medicalDocumentStorageService,
            MedicalFieldExtractionService medicalFieldExtractionService,
            MedicalFieldPostProcessor medicalFieldPostProcessor,
            MedicalDocumentRenderService medicalDocumentRenderService,
            MedicalDocumentRepository medicalDocumentRepository,
            ObjectMapper objectMapper) {
        this.medicalAccessService = medicalAccessService;
        this.medicalDocumentAnalysisService = medicalDocumentAnalysisService;
        this.medicalDocumentStorageService = medicalDocumentStorageService;
        this.medicalFieldExtractionService = medicalFieldExtractionService;
        this.medicalFieldPostProcessor = medicalFieldPostProcessor;
        this.medicalDocumentRenderService = medicalDocumentRenderService;
        this.medicalDocumentRepository = medicalDocumentRepository;
        this.objectMapper = objectMapper;
    }

    public MedicalSmartRecognitionResultDto recognizeAndArchive(
            AuthPrincipal principal, Long elderProfileId, MultipartFile file) {
        long startNs = System.nanoTime();
        long elderId = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        log.info(
                "medical.smart_recognition.start elderId={} userId={} role={} filename={} contentType={}",
                elderId,
                principal.userId(),
                principal.role(),
                originalFilename,
                contentType);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(4001, "读取上传文件失败：" + e.getMessage());
        }

        int inputBytes = bytes == null ? 0 : bytes.length;
        log.info("medical.smart_recognition.uploaded elderId={} inputBytes={}", elderId, inputBytes);

        MedicalOcrView ocr = medicalDocumentAnalysisService.analyze(bytes);
        int fullTextChars = ocr.getFullText() == null ? 0 : ocr.getFullText().length();
        log.info(
                "medical.smart_recognition.ocr_done elderId={} fullTextChars={} structuredApi={} routeSource={}",
                elderId,
                fullTextChars,
                ocr.getRoutedSpecializedApi(),
                ocr.getStructuredRouteSource());

        String storedPath;
        try {
            storedPath = medicalDocumentStorageService.save(elderId, originalFilename, bytes);
        } catch (IOException e) {
            throw new ApiException(5001, "保存单据图片失败：" + e.getMessage());
        }
        log.info("medical.smart_recognition.saved_image elderId={} storedPath={}", elderId, storedPath);

        MedicalFieldPostProcessor.ProcessedText processedText = medicalFieldPostProcessor.process(ocr.getFullText());
        MedicalOcrView normalizedOcr =
                MedicalOcrView.builder()
                        .fullText(processedText.getCleanedText())
                        .raw(ocr.getRaw())
                        .documentClasses(ocr.getDocumentClasses())
                        .classifyRaw(ocr.getClassifyRaw())
                        .routedSpecializedApi(ocr.getRoutedSpecializedApi())
                        .specializedRaw(ocr.getSpecializedRaw())
                .structuredError(ocr.getStructuredError())
                        .classificationWarning(ocr.getClassificationWarning())
                        .structuredRouteSource(ocr.getStructuredRouteSource())
                        .build();

        int cleanedChars = normalizedOcr.getFullText() == null ? 0 : normalizedOcr.getFullText().length();
        log.info(
                "medical.smart_recognition.postprocess elderId={} inputChars={} cleanedChars={} deltaChars={}",
                elderId,
                fullTextChars,
                cleanedChars,
                cleanedChars - fullTextChars);

        ExtractedMedicalFieldsDto extracted = medicalFieldExtractionService.extract(normalizedOcr);
        List<SuggestedCalendarEventDto> suggested = medicalFieldExtractionService.suggestEvents(extracted, normalizedOcr);
        MedicalDocument renderDoc = new MedicalDocument();
        renderDoc.setDocCategory(extracted.getDocCategory());
        MedicalDocumentRenderService.RenderedDocument rendered =
            medicalDocumentRenderService.renderFromStored(
                renderDoc,
                normalizedOcr,
                extracted,
                null,
                null,
                normalizedOcr.getStructuredError());

        int detectedDateCount = extracted.getDetectedDateTexts() == null ? 0 : extracted.getDetectedDateTexts().size();
        int normalizedDateCount = extracted.getNormalizedDates() == null ? 0 : extracted.getNormalizedDates().size();
        int keywordCount = extracted.getMatchedKeywords() == null ? 0 : extracted.getMatchedKeywords().size();
        log.info(
                "medical.smart_recognition.extracted elderId={} docCategory={} detectedDates={} normalizedDates={} matchedKeywords={} suggestedEvents={}",
                elderId,
                extracted.getDocCategory(),
                detectedDateCount,
                normalizedDateCount,
                keywordCount,
                suggested.size());

        MedicalDocument doc = new MedicalDocument();
        doc.setElderProfileId(elderId);
        doc.setUploadedByUserId(principal.userId());
        doc.setOriginalFilename(originalFilename);
        doc.setStoredPath(storedPath);
        doc.setContentType(contentType);
        doc.setDocCategory(extracted.getDocCategory());
        doc.setRoutedSpecializedApi(ocr.getRoutedSpecializedApi());
        doc.setStructuredRouteSource(ocr.getStructuredRouteSource());
        doc.setStructuredError(rendered.structuredError());
        doc.setFullText(ocr.getFullText());
        doc.setTitle(buildDefaultTitle(normalizedOcr.getFullText()));
        doc.setOcrRawJson(writeJson(ocr.getRaw()));
        doc.setClassifyRawJson(writeJson(ocr.getClassifyRaw()));
        doc.setSpecializedRawJson(writeJson(ocr.getSpecializedRaw()));
        doc.setDisplayBlocksJson(writeJson(rendered.displayBlocks()));
        doc.setStructuredFieldsJson(writeJson(rendered.structuredFields()));
        try {
            doc.setExtractedFieldsJson(objectMapper.writeValueAsString(extracted));
        } catch (JsonProcessingException e) {
            throw new ApiException(5001, "序列化抽取字段失败");
        }

        doc = medicalDocumentRepository.save(doc);

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        log.info(
                "medical.smart_recognition.done elderId={} documentId={} elapsedMs={} storedPath={} docCategory={} fullTextChars={} cleanedChars={} suggestedEvents={}",
                elderId,
                doc.getId(),
                elapsedMs,
                storedPath,
                extracted.getDocCategory(),
                fullTextChars,
                cleanedChars,
                suggested.size());

        // prepare variants for front-end: include specializedRaw and tableRows when available
        Map<String, Object> variants = Map.of(
            "specializedRaw", ocr.getSpecializedRaw(),
            "tableRows", medicalDocumentRenderService.extractTableRowsFromSpecializedRaw(ocr.getSpecializedRaw())
        );

        return MedicalSmartRecognitionResultDto.builder()
            .documentId(doc.getId())
            .ocr(ocr)
            .extractedFields(extracted)
            .suggestedCalendarEvents(suggested)
            .displayBlocks(rendered.displayBlocks())
            .structuredFields(rendered.structuredFields())
            .structuredError(rendered.structuredError())
            .variants(variants)
            .build();
    }

    private String buildDefaultTitle(String fullText) {
        if (fullText == null || fullText.isBlank()) {
            return "医疗单据";
        }
        String firstLine = fullText.split("\\R", 2)[0].trim();
        if (firstLine.length() > 80) {
            return firstLine.substring(0, 80) + "…";
        }
        return firstLine.isEmpty() ? "医疗单据" : firstLine;
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}