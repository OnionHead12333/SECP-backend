package com.smartelderly.api.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.ai.dto.AiTrainingJobResponse;
import com.smartelderly.api.ai.dto.AiTrainingRequest;
import com.smartelderly.api.ai.dto.AiTrainingResponse;
import com.smartelderly.api.ai.dto.AiTrainingStatusResponse;
import com.smartelderly.api.ai.service.AiTrainingService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/admin/ai/train")
public class AiTrainingController {

    private final AiTrainingService aiTrainingService;

    public AiTrainingController(AiTrainingService aiTrainingService) {
        this.aiTrainingService = aiTrainingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AiTrainingResponse>> train(@Valid @RequestBody AiTrainingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("训练已开始", aiTrainingService.train(request)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AiTrainingStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiTrainingService.getStatus()));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<AiTrainingJobResponse>> getJob(@org.springframework.web.bind.annotation.PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiTrainingService.getJob(jobId)));
    }
}
