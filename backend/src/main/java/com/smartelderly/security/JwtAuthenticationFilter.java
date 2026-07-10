package com.smartelderly.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.ApiResponse;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper, Environment environment) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            // 前端存在 demo token（仅用于本地联调）。只在 dev profile 下启用，避免污染生产。
            // - demo-child -> userId=123123, role=child
            // - demo-elder-<digits> -> userId=<digits>, role=elder
            if (isDevProfile() && token.startsWith("demo-")) {
                AuthPrincipal principal = tryParseDemoToken(token);
                if (principal != null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            AuthorityUtils.createAuthorityList("ROLE_" + principal.role().name()));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    filterChain.doFilter(request, response);
                    return;
                }
            }
            var claims = jwtService.parseClaims(token);
            AuthPrincipal principal = jwtService.toPrincipal(claims);
            var auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_" + principal.role().name()));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT invalid: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.fail(4011, "token expired"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isDevProfile() {
        for (String p : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(p)) {
                return true;
            }
        }
        return false;
    }

    private static AuthPrincipal tryParseDemoToken(String token) {
        if ("demo-child".equals(token)) {
            return new AuthPrincipal(123123L, com.smartelderly.domain.UserRole.child);
        }
        String prefix = "demo-elder-";
        if (token.startsWith(prefix)) {
            String digits = token.substring(prefix.length()).trim();
            if (digits.isEmpty()) {
                return null;
            }
            for (int i = 0; i < digits.length(); i++) {
                char c = digits.charAt(i);
                if (c < '0' || c > '9') {
                    return null;
                }
            }
            long userId = Long.parseLong(digits);
            return new AuthPrincipal(userId, com.smartelderly.domain.UserRole.elder);
        }
        return null;
    }
}
