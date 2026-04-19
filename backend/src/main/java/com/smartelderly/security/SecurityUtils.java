package com.smartelderly.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.smartelderly.api.ApiException;
import com.smartelderly.domain.UserRole;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    // 获取当前登录用户的认证信息，如果没有认证或认证信息无效，则抛出401异常
    public static AuthPrincipal requireAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
}
