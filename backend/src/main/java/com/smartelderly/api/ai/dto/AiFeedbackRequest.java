package com.smartelderly.api.ai.dto;

import lombok.Data;

@Data
public class AiFeedbackRequest {

    private String feedbackType;
    private String feedbackText;
    private Boolean isHelpful;
    private Boolean hasVisitedDoctor;
}
