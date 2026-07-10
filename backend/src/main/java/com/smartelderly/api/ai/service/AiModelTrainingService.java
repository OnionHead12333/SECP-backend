package com.smartelderly.api.ai.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.AiModelStatusResponse;
import com.smartelderly.api.ai.dto.AiModelTrainRequest;
import com.smartelderly.api.ai.dto.AiModelTrainResponse;

@Service
public class AiModelTrainingService {

    private final Map<String, String> status = new ConcurrentHashMap<>();

    public AiModelTrainingService() {
        status.put("risk", "not_trained");
        status.put("department", "not_trained");
    }

    public AiModelTrainResponse trainRiskModel(AiModelTrainRequest request) {
        return runPythonTrain("risk", "train_risk_model.py", request);
    }

    public AiModelTrainResponse trainDepartmentModel(AiModelTrainRequest request) {
        return runPythonTrain("department", "train_department_model.py", request);
    }

    public AiModelStatusResponse getStatus() {
        Path root = Path.of("backend", "data", "ai-models");
        return AiModelStatusResponse.builder()
                .riskModelStatus(status.getOrDefault("risk", "not_trained"))
                .departmentModelStatus(status.getOrDefault("department", "not_trained"))
                .riskModelPath(root.resolve("risk_triage_model.pkl").toString())
                .departmentModelPath(root.resolve("department_model.pkl").toString())
                .message("操作成功")
                .build();
    }

    private AiModelTrainResponse runPythonTrain(String modelType, String scriptName, AiModelTrainRequest request) {
        try {
            Path script = Path.of("ai_scripts", scriptName);
            if (!Files.exists(script)) {
                status.put(modelType, "script_missing");
                return AiModelTrainResponse.builder()
                        .success(false)
                        .message("训练脚本不存在: " + script)
                        .modelType(modelType)
                        .build();
            }
            Path outputDir = Path.of(request == null || request.getModelOutputDir() == null ? "backend/data/ai-models" : request.getModelOutputDir());
            Files.createDirectories(outputDir);
            String python = System.getenv().getOrDefault("PYTHON", "python");
            String datasetPath = request == null || request.getDatasetPath() == null
                    ? Path.of("data", "medical_qa_dataset.csv").toString()
                    : request.getDatasetPath();
            ProcessBuilder pb = new ProcessBuilder(
                    python,
                    script.toString(),
                    datasetPath,
                    outputDir.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            int exit = process.waitFor();
            if (exit == 0) {
                status.put(modelType, "trained");
                String modelFile = modelType.equals("risk") ? "risk_triage_model.pkl" : "department_model.pkl";
                String vectorizerFile = modelType.equals("risk") ? "risk_triage_vectorizer.pkl" : "department_vectorizer.pkl";
                return AiModelTrainResponse.builder()
                        .success(true)
                        .message("训练成功")
                        .modelType(modelType)
                        .modelPath(outputDir.resolve(modelFile).toString())
                        .vectorizerPath(outputDir.resolve(vectorizerFile).toString())
                        .accuracy(parseMetric(out.toString(), "accuracy"))
                        .macroF1(parseMetric(out.toString(), "macro_f1"))
                        .build();
            }
            status.put(modelType, "failed");
            return AiModelTrainResponse.builder()
                    .success(false)
                    .message("训练失败: " + out)
                    .modelType(modelType)
                    .build();
        } catch (Exception e) {
            status.put(modelType, "failed");
            return AiModelTrainResponse.builder()
                    .success(false)
                    .message("训练异常: " + e.getMessage())
                    .modelType(modelType)
                    .build();
        }
    }

    private Double parseMetric(String text, String key) {
        try {
            for (String line : text.split("\\R")) {
                if (line.startsWith(key + ":")) {
                    return Double.parseDouble(line.substring(key.length() + 1).trim());
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
