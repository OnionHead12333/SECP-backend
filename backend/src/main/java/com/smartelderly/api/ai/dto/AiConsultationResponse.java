package com.smartelderly.api.ai.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiConsultationResponse {

    private Long consultationId;
    private String symptomType;
    private String riskLevel;
    private Boolean needMedicalVisit;
    private Boolean needFamilyNotify;
    private String finalAnswer;
    private List<RecommendedDepartmentDTO> recommendedDepartments;
    private List<MatchedQaDTO> matchedQaList;
    private String followUpQuestion;
    private String safetyNotice;
    private String status;
}
