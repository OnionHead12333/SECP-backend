package com.smartelderly.service.ocr;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.MedicalOcrView;

/**
 * 调用百度云「通用文字识别（高精度版）」同步接口，将图片字节识别为文本。
 *
 * @see <a href="https://cloud.baidu.com/doc/OCR/s/1k69z17zc">高精度版文档</a>
 */
@Service
public class BaiduAccurateBasicOcrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduAccurateBasicOcrService.class);

    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic";

    private final BaiduOcrAccessTokenService tokenService;
    private final ObjectMapper objectMapper;

    public BaiduAccurateBasicOcrService(BaiduOcrAccessTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    public MedicalOcrView recognizeBytes(byte[] imageBytes) {
        tokenService.requireAccessToken();
        if (imageBytes == null || imageBytes.length == 0) {
            throw new ApiException(4001, "图片为空");
        }
        if (imageBytes.length > 4 * 1024 * 1024) {
            throw new ApiException(4001, "图片过大（百度云同步接口建议不超过约 4MB），请先裁剪压缩后再上传");
        }

        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
        String body =
                "image=" + URLEncoder.encode(base64, StandardCharsets.UTF_8) + "&detect_direction=true";

        String token = tokenService.requireAccessToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OCR_URL + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = tokenService
                    .httpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                log.warn("Baidu OCR HTTP {} body {}", resp.statusCode(), resp.body());
                throw new ApiException(5020, "百度云 OCR 请求失败（HTTP " + resp.statusCode() + "）");
            }
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.hasNonNull("error_code") && root.path("error_code").asInt() != 0) {
                String msg = root.path("error_msg").asText("unknown");
                log.warn("Baidu OCR error: {} {}", root.path("error_code").asText(), msg);
                throw new ApiException(5021, "百度云 OCR：" + msg);
            }
            String fullText = extractFullText(root);
            Map<String, Object> raw =
                    objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
            return MedicalOcrView.builder().fullText(fullText).raw(raw).build();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Baidu OCR call failed", e);
            throw new ApiException(5022, "调用百度云 OCR 异常：" + e.getMessage());
        }
    }

    private static String extractFullText(JsonNode root) {
        JsonNode arr = root.path("words_result");
        if (!arr.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode item : arr) {
            String w = item.path("words").asText("").trim();
            if (w.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(w);
        }
        return sb.toString();
    }
}
