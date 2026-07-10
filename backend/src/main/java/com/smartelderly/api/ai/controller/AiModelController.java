package com.smartelderly.api.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.ai.dto.AiModelPredictRequest;
import com.smartelderly.api.ai.dto.AiModelPredictResponse;
import com.smartelderly.api.ai.dto.AiModelStatusResponse;
import com.smartelderly.api.ai.dto.AiModelTrainRequest;
import com.smartelderly.api.ai.dto.AiModelTrainResponse;
import com.smartelderly.api.ai.service.AiModelPredictionService;
import com.smartelderly.api.ai.service.AiModelTrainingService;

@RestController
@RequestMapping("/admin/ai/models")
public class AiModelController {

    private final AiModelTrainingService trainingService;
    private final AiModelPredictionService predictionService;

    public AiModelController(AiModelTrainingService trainingService, AiModelPredictionService predictionService) {
        this.trainingService = trainingService;
        this.predictionService = predictionService;
    }

    @PostMapping("/train-risk")
    public ResponseEntity<ApiResponse<AiModelTrainResponse>> trainRisk(@RequestBody(required = false) AiModelTrainRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", trainingService.trainRiskModel(request)));
    }

    @PostMapping("/train-department")
    public ResponseEntity<ApiResponse<AiModelTrainResponse>> trainDepartment(@RequestBody(required = false) AiModelTrainRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", trainingService.trainDepartmentModel(request)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AiModelStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", trainingService.getStatus()));
    }

    @PostMapping("/predict")
    public ResponseEntity<ApiResponse<AiModelPredictResponse>> predict(@RequestBody AiModelPredictRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", predictionService.predict(request)));
    }
}
