package com.smartelderly.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiConsultationCreateRequest {

    @NotNull
    private Long elderlyId;

    @NotBlank
    private String inputText;

    @NotBlank
    private String inputType;
}
