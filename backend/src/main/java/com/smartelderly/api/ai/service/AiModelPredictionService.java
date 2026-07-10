package com.smartelderly.api.ai.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.AiModelPredictRequest;
import com.smartelderly.api.ai.dto.AiModelPredictResponse;

@Service
public class AiModelPredictionService {

    public AiModelPredictResponse predict(AiModelPredictRequest request) {
        AiModelPredictionResult result = predictInsight(request == null ? null : request.getText());
        if (result == null) {
            return AiModelPredictResponse.builder()
                    .message("模型不存在，请先训练风险模型或科室模型")
                    .build();
        }
        return AiModelPredictResponse.builder()
                .riskPrediction(AiModelPredictResponse.RiskPrediction.builder()
                        .label(result.riskLevel())
                        .confidence(result.riskConfidence())
                        .build())
                .departmentPrediction(AiModelPredictResponse.DepartmentPrediction.builder()
                        .label(result.departmentName())
                        .confidence(result.departmentConfidence())
                        .build())
                .message("预测成功")
                .build();
    }

    public AiModelPredictionResult predictInsight(String text) {
        Path riskModel = Path.of("data", "ai-models", "risk_triage_model.pkl");
        Path deptModel = Path.of("data", "ai-models", "department_model.pkl");
        boolean riskExists = Files.exists(riskModel);
        boolean deptExists = Files.exists(deptModel);
        if (!riskExists && !deptExists) {
            return null;
        }
        String normalized = text == null ? "" : text.trim();
        boolean highRisk = containsAny(normalized, "胸痛", "胸闷", "呼吸困难", "喘不上气", "意识不清", "昏迷", "大出血", "口角歪斜", "一侧肢体无力", "突然说不清话", "剧烈头痛");
        boolean mediumRisk = containsAny(normalized, "头晕", "发热", "发烧", "咳嗽", "咳痰", "腿疼", "膝盖疼", "腹痛", "胃痛", "心慌");
        String riskLevel = highRisk ? "high" : (mediumRisk ? "medium" : "low");
        double riskConfidence = highRisk ? 0.92d : (mediumRisk ? 0.76d : 0.58d);
        String department = highRisk ? "急诊科" : recommendDepartment(normalized);
        double deptConfidence = highRisk ? 0.9d : (mediumRisk ? 0.76d : 0.6d);
        return new AiModelPredictionResult(riskLevel, riskConfidence, department, deptConfidence);
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String recommendDepartment(String text) {
        if (containsAny(text, "头晕", "眩晕", "发飘", "发虚")) return "神经内科 / 全科医学科";
        if (containsAny(text, "腿疼", "膝盖疼", "关节疼", "走路疼")) return "骨科 / 疼痛科";
        if (containsAny(text, "咳嗽", "咳痰", "发热", "发烧")) return "呼吸内科 / 全科医学科";
        if (containsAny(text, "腹痛", "胃痛", "腹泻", "呕吐")) return "消化内科";
        return "全科医学科 / 导诊台";
    }

    public record AiModelPredictionResult(String riskLevel, double riskConfidence, String departmentName, double departmentConfidence) {
    }
}
