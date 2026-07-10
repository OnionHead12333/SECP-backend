package com.smartelderly.api.ai.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiConsultationHistoryItemDTO {
    private Long consultationId;
    private String createdAt;
    private String inputText;
    private String riskLevel;
    private Boolean needMedicalVisit;
    private Boolean needFamilyNotify;
    private List<RecommendedDepartmentDTO> recommendedDepartments;
    private String finalAnswerSummary;
    private String status;
}
