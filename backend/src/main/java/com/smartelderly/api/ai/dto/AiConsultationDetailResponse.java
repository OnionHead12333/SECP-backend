package com.smartelderly.api.ai.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiConsultationDetailResponse {

    private Long consultationId;
    private Long elderlyId;
    private String inputText;
    private String inputType;
    private String riskLevel;
    private Boolean needMedicalVisit;
    private Boolean needFamilyNotify;
    private String finalAnswer;
    private List<RecommendedDepartmentDTO> recommendedDepartments;
    private List<MatchedQaDTO> matchedQaList;
    private String followUpQuestion;
    private String safetyNotice;
    private String status;
    private String createdAt;
}
