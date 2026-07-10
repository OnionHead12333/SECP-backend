package com.smartelderly.api.dto.community;

import java.util.List;

public record DirectMessagePageResponse(List<DirectMessageResponse> items, boolean hasMore, Long threadId,
        String peerScopeKey) {
}