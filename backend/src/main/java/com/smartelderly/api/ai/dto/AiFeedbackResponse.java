package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiFeedbackResponse {
    private Long consultationId;
    private boolean success;
    private String message;
}
