package com.smartelderly.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.smartelderly.api.ApiException;
import com.smartelderly.domain.UserRole;

public final class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    private SecurityUtils() {
    }

    // 获取当前登录用户的认证信息，如果没有认证或认证信息无效，则抛出401异常
    public static AuthPrincipal requireAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.debug("requireAuth: authentication is null");
        } else {
            Object principal = authentication.getPrincipal();
            log.debug("requireAuth: authClass={}, authenticated={}, principalClass={}, principal={}",
                    authentication.getClass().getName(),
                    authentication.isAuthenticated(),
                    principal == null ? null : principal.getClass().getName(),
                    principal);
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(4010, "unauthorized");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthPrincipal authPrincipal)) {
            throw new ApiException(4010, "unauthorized");
        }
        return authPrincipal;
    }

    // 验证当前登录用户的角色是否为expected，如果不是则抛出403异常
    public static AuthPrincipal requireRole(UserRole expected) {
        AuthPrincipal p = requireAuth();
        if (p.role() != expected) {
            throw new ApiException(4030, "forbidden");
        }
        return p;
    }

    public static AuthPrincipal requireMatchingUserId(Long requestedUserId) {
        AuthPrincipal principal = requireAuth();
        requireMatchingUserId(principal, requestedUserId);
        return principal;
    }

    public static void requireMatchingUserId(AuthPrincipal principal, Long requestedUserId) {
        if (requestedUserId != null && requestedUserId.longValue() != principal.userId()) {
            throw new ApiException(4030, "forbidden");
        }
    }
}
