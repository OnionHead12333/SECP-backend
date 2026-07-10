package com.smartelderly.api.ai.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.MatchedQaDTO;
import com.smartelderly.api.ai.dto.RecommendedDepartmentDTO;
import com.smartelderly.domain.ai.AiMedicalQaKnowledge;

@Service
public class AiRiskRuleService {

    private static final List<String> DEFAULT_HIGH_RISK_KEYWORDS = List.of(
            "胸痛", "胸闷", "呼吸困难", "喘不上气", "意识不清", "昏迷", "突然说不清话", "口角歪斜", "一侧肢体无力", "偏瘫", "剧烈头痛", "大出血", "便血", "黑便", "摔倒后不能动", "持续高热", "抽搐", "心悸严重", "血压特别高");

    public RiskDecision decide(String inputText) {
        String normalized = inputText == null ? "" : inputText.toLowerCase(Locale.ROOT);
        boolean highRisk = containsAny(normalized, DEFAULT_HIGH_RISK_KEYWORDS.toArray(String[]::new));
        if (highRisk) {
            return new RiskDecision(
                    "high",
                    true,
                    true,
                    "您描述的症状可能存在较高风险，建议尽快就医。",
                    List.of(
                            RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("急诊科").reason("高危症状优先急诊评估").build(),
                            RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("相关专科").reason("按症状进一步专科评估").build()),
                    List.of(),
                    null,
                    null,
                    "命中高危症状关键词");
        }
        return new RiskDecision("low", false, false, null, List.of(), List.of(), null, null, null);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    public record RiskDecision(String riskLevel, Boolean needMedicalVisit, Boolean needFamilyNotify,
            String highRiskHint, List<RecommendedDepartmentDTO> recommendedDepartments,
            List<MatchedQaDTO> matchedQaList, String followUpQuestion, Long matchedRuleId, String matchedRuleName) {
    }
}
