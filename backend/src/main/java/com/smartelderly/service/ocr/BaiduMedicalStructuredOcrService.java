package com.smartelderly.service.ocr;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 调用百度云医疗类「结构化」识别接口（与通用 {@link BaiduAccurateBasicOcrService} 并列）。
 * <p>
 * 任一接口未开通、欠费或识别失败时返回 empty，不阻断主流程。
 */
@Service
public class BaiduMedicalStructuredOcrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduMedicalStructuredOcrService.class);

    private static final String HOST = "https://aip.baidubce.com";

    private final BaiduOcrAccessTokenService tokenService;
    private final ObjectMapper objectMapper;

    public BaiduMedicalStructuredOcrService(
            BaiduOcrAccessTokenService tokenService,
            ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * @return 解析后的 JSON Map；失败返回 empty
     */
    public Optional<Map<String, Object>> tryRecognize(SpecializedMedicalOcrApi api, byte[] imageBytes) {
        try {
            tokenService.requireAccessToken();
            if (imageBytes == null || imageBytes.length == 0) {
                return Optional.empty();
            }
            if (imageBytes.length > 8 * 1024 * 1024) {
                log.warn("structured ocr {} skipped: image too large", api.getApiId());
                return Optional.empty();
            }

            String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
            String formBody = "image=" + URLEncoder.encode(base64, StandardCharsets.UTF_8)
                    + "&location=true&probability=true";

            String token = tokenService.requireAccessToken();
            String uri = HOST + api.getPath() + "?access_token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofSeconds(75))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = tokenService
                    .httpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() != 200) {
                log.warn("structured ocr {} HTTP {} ", api.getApiId(), resp.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.hasNonNull("error_code") && root.path("error_code").asInt() != 0) {
                log.warn(
                        "structured ocr {} baidu error {} : {}",
                        api.getApiId(),
                        root.path("error_code").asText(),
                        root.path("error_msg").asText());
                return Optional.empty();
            }
            Map<String, Object> raw =
                    objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
            return Optional.of(raw);
        } catch (Exception e) {
            log.warn("structured ocr {} exception: {}", api.getApiId(), e.getMessage());
            return Optional.empty();
        }
    }
}
