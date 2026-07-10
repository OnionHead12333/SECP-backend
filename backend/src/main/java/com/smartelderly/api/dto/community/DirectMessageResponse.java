package com.smartelderly.api.dto.community;

public record DirectMessageResponse(
        String id,
        Long threadId,
        String senderScopeKey,
        String senderDisplay,
        String senderRole,
        String kind,
        String textContent,
        String audioUrl,
        String imageUrl,
        Integer durationMs,
        long createdAtMillis,
        boolean mine) {
}