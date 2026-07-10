package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTrainingJobResponse {

    private Long id;
    private String algorithm;
    private String status;
    private Integer totalSamples;
    private Integer validSamples;
    private Integer duplicateSamples;
    private String indexPath;
    private String errorMessage;
}
