package com.smartelderly.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiConsultationMessageRequest {

    @NotBlank
    private String messageContent;

    @NotBlank
    private String messageType;
}
