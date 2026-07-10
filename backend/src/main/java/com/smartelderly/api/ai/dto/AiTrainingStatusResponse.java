package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTrainingStatusResponse {
    private String modelType;
    private String status;
    private String modelName;
    private String algorithm;
    private Integer trainSamples;
    private Integer validSamples;
    private Integer labelCount;
    private String modelPath;
    private String vectorizerPath;
    private String metricsJson;
    private String startedAt;
    private String finishedAt;
    private Long durationMs;
    private String errorMessage;
    private String message;
    private Integer topK;
    private Double similarityThreshold;
    private Long trainedQaCount;
    private Long lastTrainedAtEpochMillis;
}
