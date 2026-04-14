package com.smartelderly.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.smartelderly.config.AppProperties;
import com.smartelderly.domain.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(AppProperties appProperties) {
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 characters for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseClaims(String jwt) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
    }

    public String issueToken(long userId, UserRole role, Instant expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("expiresAt must be after now");
        }
        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("role", role.name())
                .signWith(key)
                .compact();
    }

    public AuthPrincipal toPrincipal(Claims claims) {
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new JwtException("missing sub");
        }
        long userId = Long.parseLong(sub);
        String roleStr = claims.get("role", String.class);
        if (roleStr == null || roleStr.isBlank()) {
            throw new JwtException("missing role");
        }
        UserRole role = UserRole.valueOf(roleStr);
        return new AuthPrincipal(userId, role);
    }
}
