package com.smartelderly.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.DevTokenRequest;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/dev")
@Profile("dev")
@Validated
public class DevTokenController {

    private final JwtService jwtService;

    public DevTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ApiResponse<Map<String, Object>> issue(@Valid @RequestBody DevTokenRequest request) {
        UserRole role = UserRole.valueOf(request.role());
        int ttl = request.ttlSeconds() == null ? 3600 : request.ttlSeconds();
        Instant expiresAt = Instant.now().plusSeconds(ttl);
        String token = jwtService.issueToken(request.userId(), role, expiresAt);
        return ApiResponse.ok(Map.of(
                "token", token,
                "userId", request.userId(),
                "role", role.name(),
                "expiresAt", expiresAt.toString()));
    }
}

