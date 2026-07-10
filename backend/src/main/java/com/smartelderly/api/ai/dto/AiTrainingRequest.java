package com.smartelderly.api.ai.dto;

import lombok.Data;

@Data
public class AiTrainingRequest {

    private Boolean rebuildKnowledge;
    private String algorithm;
    private Integer topK;
    private Double similarityThreshold;
}
