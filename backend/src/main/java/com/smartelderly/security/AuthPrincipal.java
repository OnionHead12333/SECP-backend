package com.smartelderly.security;

import com.smartelderly.domain.UserRole;

public record AuthPrincipal(long userId, UserRole role) {
}
