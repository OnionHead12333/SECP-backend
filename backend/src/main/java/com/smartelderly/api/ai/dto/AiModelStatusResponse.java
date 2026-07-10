package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiModelStatusResponse {
    private String riskModelStatus;
    private String departmentModelStatus;
    private String riskModelPath;
    private String departmentModelPath;
    private String message;
}
