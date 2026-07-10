package com.smartelderly.api.eldercommunity.dto;

import lombok.Data;

@Data
public class ClearChatResponse {
    private String communityId;
    private String viewerScopeKey;
    private Long clearBeforeMillis;
}
