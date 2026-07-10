package com.smartelderly.api.dto.community;

public record ElderFriendCandidateResponse(
        String scopeKey,
        String displayName,
        String phone,
        String hint,
        String emoji) {
}