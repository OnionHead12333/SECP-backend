package com.smartelderly.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.smartelderly.config.AppProperties;
import com.smartelderly.domain.UserRole;

import io.jsonwebtoken.Claims;

@DisplayName("JWT 服务单元测试")
class JwtServiceTest {

    @Test
    @DisplayName("构造 JWT 服务：密钥长度不足应拒绝启动")
    void constructor_shortSecret_throwsException() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("too-short");

        assertThrows(IllegalArgumentException.class, () -> new JwtService(props));
    }

    @Test
    @DisplayName("签发并解析 token：应包含 userId 和 role")
    void issueAndParseToken_validInput_returnsExpectedClaims() {
        JwtService service = jwtService();

        String token = service.issueToken(42L, UserRole.elder, Instant.now().plusSeconds(3600));
        Claims claims = service.parseClaims(token);
        AuthPrincipal principal = service.toPrincipal(claims);

        assertEquals("42", claims.getSubject());
        assertEquals("elder", claims.get("role", String.class));
        assertEquals(42L, principal.userId());
        assertEquals(UserRole.elder, principal.role());
    }

    @Test
    @DisplayName("签发 token：过期时间为空应抛出异常")
    void issueToken_nullExpiresAt_throwsException() {
        JwtService service = jwtService();

        assertThrows(IllegalArgumentException.class,
                () -> service.issueToken(1L, UserRole.child, null));
    }

    @Test
    @DisplayName("签发 token：过期时间早于当前时间应抛出异常")
    void issueToken_expiredTime_throwsException() {
        JwtService service = jwtService();

        assertThrows(IllegalArgumentException.class,
                () -> service.issueToken(1L, UserRole.child, Instant.now().minusSeconds(1)));
    }

    private static JwtService jwtService() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("unit-test-secret-key-at-least-32-chars!!");
        return new JwtService(props);
    }
}
