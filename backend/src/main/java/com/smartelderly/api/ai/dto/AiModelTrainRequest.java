package com.smartelderly.api.ai.dto;

import lombok.Data;

@Data
public class AiModelTrainRequest {
    private String datasetPath;
    private String modelOutputDir;
    private String algorithm;
}
