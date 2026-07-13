package com.smartelderly.api.control;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpRobotGatewayClient implements RobotGatewayClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RobotGatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public HttpRobotGatewayClient(RobotGatewayProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build());
    }

    HttpRobotGatewayClient(RobotGatewayProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public RobotGatewayCommandResponse sendCommand(RobotGatewayCommandRequest request) {
        String body = toJson(request);
        HttpRequest httpRequest = HttpRequest.newBuilder(uri("/api/command"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = send(httpRequest);
        return parseCommandResponse(response);
    }

    @Override
    public Map<String, Object> fetchState() {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri("/api/state"))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = send(httpRequest);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RobotGatewayUnavailableException("gateway state returned " + response.statusCode());
        }
        return parseStateResponse(response.body());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RobotGatewayUnavailableException("gateway unavailable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RobotGatewayUnavailableException("gateway unavailable", e);
        }
    }

    private RobotGatewayCommandResponse parseCommandResponse(HttpResponse<String> response) {
        String body = response.body();
        if (body == null || body.isBlank()) {
            return response.statusCode() >= 200 && response.statusCode() < 300
                    ? RobotGatewayCommandResponse.success("ok", null)
                    : RobotGatewayCommandResponse.failure("控制失败", null);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data").isObject() ? root.path("data") : root;
            boolean success = isCommandSuccess(root, data, response.statusCode());
            String message = firstText(root, data, "message", success ? "ok" : "控制失败");
            String controlBlockReason = firstText(root, data, "control_block_reason", null);
            if (controlBlockReason == null) {
                controlBlockReason = firstText(root, data, "controlBlockReason", null);
            }
            return new RobotGatewayCommandResponse(success, message, controlBlockReason);
        } catch (JsonProcessingException e) {
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return new RobotGatewayCommandResponse(success, success ? "ok" : "控制失败", null);
        }
    }

    private Map<String, Object> parseStateResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data").isObject() ? root.path("data") : root;
            return objectMapper.convertValue(data, MAP_TYPE);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return new LinkedHashMap<>();
        }
    }

    private boolean isCommandSuccess(JsonNode root, JsonNode data, int statusCode) {
        if (statusCode < 200 || statusCode >= 300) {
            return false;
        }
        if (root.has("success")) {
            return root.path("success").asBoolean();
        }
        if (root.has("ok")) {
            return root.path("ok").asBoolean();
        }
        if (root.has("code") && root.path("code").canConvertToInt()) {
            return root.path("code").asInt() == 0;
        }
        String status = firstText(root, data, "status", null);
        return status == null
                || (!"failed".equalsIgnoreCase(status) && !"blocked".equalsIgnoreCase(status));
    }

    private URI uri(String path) {
        String baseUrl = properties.getBaseUrl();
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalized + path);
    }

    private String toJson(RobotGatewayCommandRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid robot gateway command", e);
        }
    }

    private static String firstText(JsonNode root, JsonNode data, String fieldName, String fallback) {
        if (root.hasNonNull(fieldName)) {
            return root.path(fieldName).asText();
        }
        if (data.hasNonNull(fieldName)) {
            return data.path(fieldName).asText();
        }
        return fallback;
    }
}
