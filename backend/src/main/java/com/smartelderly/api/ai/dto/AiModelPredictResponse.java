package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiModelPredictResponse {
    private RiskPrediction riskPrediction;
    private DepartmentPrediction departmentPrediction;
    private String message;

    @Data
    @Builder
    public static class RiskPrediction {
        private String label;
        private Double confidence;
    }

    @Data
    @Builder
    public static class DepartmentPrediction {
        private String label;
        private Double confidence;
    }
}
