package com.smartelderly.api.dto.community;

public record ElderFriendResponse(
        String scopeKey,
        String displayName,
        String phone,
        String hint,
        String emoji,
        long addedAtMillis) {
}