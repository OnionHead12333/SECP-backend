package com.smartelderly.service.ocr;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.ApiException;
import com.smartelderly.config.AppProperties;

/** 百度云 OCR 共用 access_token（带本地缓存）。 */
@Service
public class BaiduOcrAccessTokenService {

    private static final Logger log = LoggerFactory.getLogger(BaiduOcrAccessTokenService.class);

    static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Object tokenLock = new Object();
    private volatile String cachedAccessToken;
    private volatile Instant tokenValidUntil = Instant.EPOCH;

    public BaiduOcrAccessTokenService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String requireAccessToken() {
        if (!appProperties.getBaidu().getOcr().isConfigured()) {
            throw new ApiException(
                    5030,
                    "服务端未配置百度云 OCR，请在环境变量中设置 BAIDU_OCR_API_KEY、BAIDU_OCR_SECRET_KEY");
        }
        Instant now = Instant.now();
        if (cachedAccessToken != null && now.isBefore(tokenValidUntil.minusSeconds(120))) {
            return cachedAccessToken;
        }
        synchronized (tokenLock) {
            now = Instant.now();
            if (cachedAccessToken != null && now.isBefore(tokenValidUntil.minusSeconds(120))) {
                return cachedAccessToken;
            }
            String apiKey = appProperties.getBaidu().getOcr().getApiKey().trim();
            String secret = appProperties.getBaidu().getOcr().getSecretKey().trim();
            String qs = "grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL + "?" + qs))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            try {
                HttpResponse<String> resp =
                        httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() != 200) {
                    throw new ApiException(5023, "获取百度云 access_token 失败（HTTP " + resp.statusCode() + "）");
                }
                JsonNode root = objectMapper.readTree(resp.body());
                if (root.hasNonNull("error")) {
                    throw new ApiException(
                            5023, "获取百度云 access_token 失败：" + root.path("error_description").asText());
                }
                String token = root.path("access_token").asText(null);
                if (token == null || token.isBlank()) {
                    throw new ApiException(5023, "百度云未返回 access_token");
                }
                long expiresIn = root.path("expires_in").asLong(2592000L);
                cachedAccessToken = token;
                tokenValidUntil = Instant.now().plusSeconds(Math.max(60L, expiresIn));
                return token;
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                log.error("fetch Baidu token failed", e);
                throw new ApiException(5023, "获取百度云 access_token 异常：" + e.getMessage());
            }
        }
    }

    HttpClient httpClient() {
        return httpClient;
    }
}
