package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiModelTrainResponse {
    private boolean success;
    private String message;
    private String modelType;
    private String modelPath;
    private String vectorizerPath;
    private Double accuracy;
    private Double macroF1;
}
