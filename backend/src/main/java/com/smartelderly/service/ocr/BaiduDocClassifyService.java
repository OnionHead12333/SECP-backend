package com.smartelderly.service.ocr;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.DocumentClassItem;

/**
 * 百度云「文件检测分类」：返回图中票据/文档类别（可多主体）。
 *
 * @see <a href="https://cloud.baidu.com/doc/OCR/s/qlor1ahik">文件检测分类</a>
 */
@Service
public class BaiduDocClassifyService {

    private static final Logger log = LoggerFactory.getLogger(BaiduDocClassifyService.class);

    private static final String CLASSIFY_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/doc_classify";

    private final BaiduOcrAccessTokenService tokenService;
    private final ObjectMapper objectMapper;

    public BaiduDocClassifyService(BaiduOcrAccessTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用分类接口；失败时抛出 {@link ApiException}（由上层决定是否降级）。
     */
    public ClassifyResult classifyBytes(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new ApiException(4001, "图片为空");
        }
        if (imageBytes.length > 10 * 1024 * 1024) {
            throw new ApiException(4001, "图片过大（文件检测分类接口建议不超过约 10MB）");
        }

        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
        String body = "image=" + URLEncoder.encode(base64, StandardCharsets.UTF_8);

        String token = tokenService.requireAccessToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        CLASSIFY_URL + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = tokenService
                    .httpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                log.warn("Baidu doc_classify HTTP {} body {}", resp.statusCode(), resp.body());
                throw new ApiException(5024, "百度云文件分类请求失败（HTTP " + resp.statusCode() + "）");
            }
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.hasNonNull("error_code") && root.path("error_code").asInt() != 0) {
                String msg = root.path("error_msg").asText("unknown");
                log.warn("Baidu doc_classify error: {} {}", root.path("error_code").asText(), msg);
                throw new ApiException(5025, "百度云文件分类：" + msg);
            }

            List<DocumentClassItem> items = new ArrayList<>();
            JsonNode arr = root.path("words_result");
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    String type = node.path("type").asText("").trim();
                    double prob = node.path("probablity").asDouble(Double.NaN);
                    JsonNode loc = node.path("location");
                    Map<String, Object> locMap = null;
                    if (loc.isObject() && !loc.isEmpty()) {
                        locMap = objectMapper.convertValue(loc, new TypeReference<LinkedHashMap<String, Object>>() {});
                    }
                    items.add(DocumentClassItem.builder()
                            .type(type.isEmpty() ? "未知" : type)
                            .probability(Double.isNaN(prob) ? null : prob)
                            .location(locMap)
                            .build());
                }
            }

            Map<String, Object> raw =
                    objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
            return new ClassifyResult(items, raw);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Baidu doc_classify failed", e);
            throw new ApiException(5026, "调用百度云文件分类异常：" + e.getMessage());
        }
    }

    /** 分类失败时不抛错，便于与 OCR 组合时降级为「仅文字识别」。 */
    public ClassifyResult tryClassifyBytes(byte[] imageBytes) {
        try {
            return classifyBytes(imageBytes);
        } catch (Exception e) {
            log.warn("doc_classify failed (continuing without class): {}", e.getMessage());
            return ClassifyResult.empty();
        }
    }

    public record ClassifyResult(List<DocumentClassItem> items, Map<String, Object> raw) {
        static ClassifyResult empty() {
            return new ClassifyResult(List.of(), Map.of());
        }

        boolean isEmpty() {
            return items == null || items.isEmpty();
        }
    }
}
