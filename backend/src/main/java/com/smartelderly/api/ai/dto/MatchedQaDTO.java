package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchedQaDTO {

    private String question;
    private String answer;
    private Double similarity;
}
