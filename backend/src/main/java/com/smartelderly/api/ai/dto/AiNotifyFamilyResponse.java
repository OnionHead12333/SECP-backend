package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiNotifyFamilyResponse {
    private Long consultationId;
    private boolean success;
    private String message;
    private String notifyChannel;
    private Boolean hasFamilyBinding;
}
