package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTrainingResponse {

    private Long jobId;
    private Boolean success;
    private String message;
    private String algorithm;
    private Integer topK;
    private Double similarityThreshold;
    private Long trainedQaCount;
    private Long trainedAtEpochMillis;
}
