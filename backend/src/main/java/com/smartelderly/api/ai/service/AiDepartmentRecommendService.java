package com.smartelderly.api.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.RecommendedDepartmentDTO;
import com.smartelderly.domain.ai.AiMedicalQaKnowledge;

@Service
public class AiDepartmentRecommendService {

    public List<RecommendedDepartmentDTO> recommendByRiskAndKnowledge(AiRiskRuleService.RiskDecision riskDecision,
            List<AiMedicalQaKnowledge> matchedKnowledge) {
        if (riskDecision != null && riskDecision.recommendedDepartments() != null && !riskDecision.recommendedDepartments().isEmpty()) {
            return riskDecision.recommendedDepartments();
        }
        if (matchedKnowledge != null) {
            for (AiMedicalQaKnowledge knowledge : matchedKnowledge) {
                if (knowledge.getDepartmentId() != null) {
                    return List.of(RecommendedDepartmentDTO.builder()
                            .departmentId(knowledge.getDepartmentId())
                            .departmentName("推荐科室")
                            .reason("来自知识库匹配")
                            .build());
                }
            }
        }
        return List.of(
                RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("全科医学科").reason("适合初步评估").build(),
                RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("内科").reason("常见内科症状初筛").build(),
                RecommendedDepartmentDTO.builder().departmentId(3L).departmentName("导诊台").reason("不确定时先导诊").build());
    }
}
