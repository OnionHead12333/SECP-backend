package com.smartelderly.api.ai.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.ai.dto.AiConsultationCreateRequest;
import com.smartelderly.api.ai.dto.AiConsultationDetailResponse;
import com.smartelderly.api.ai.dto.AiConsultationHistoryItemDTO;
import com.smartelderly.api.ai.dto.AiConsultationMessageRequest;
import com.smartelderly.api.ai.dto.AiConsultationResponse;
import com.smartelderly.api.ai.dto.AiFeedbackRequest;
import com.smartelderly.api.ai.dto.AiFeedbackResponse;
import com.smartelderly.api.ai.dto.AiNotifyFamilyResponse;
import com.smartelderly.api.ai.dto.RecommendedDepartmentDTO;
import com.smartelderly.api.ai.service.AiConsultationService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/elderly/ai/consultations")
public class AiConsultationController {

    private final AiConsultationService aiConsultationService;

    public AiConsultationController(AiConsultationService aiConsultationService) {
        this.aiConsultationService = aiConsultationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AiConsultationResponse>> create(@Valid @RequestBody AiConsultationCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiConsultationService.createConsultation(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AiConsultationDetailResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiConsultationService.getConsultationDetail(id)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<AiConsultationResponse>> addMessage(
            @PathVariable Long id,
            @Valid @RequestBody AiConsultationMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiConsultationService.addMessage(id, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AiConsultationHistoryItemDTO>>> history(
            @RequestParam Long elderlyId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiConsultationService.listConsultations(elderlyId, page, pageSize)));
    }

    @PostMapping("/{id}/notify-family")
    public ResponseEntity<ApiResponse<AiNotifyFamilyResponse>> notifyFamily(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiConsultationService.notifyFamily(id)));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<ApiResponse<AiFeedbackResponse>> feedback(@PathVariable Long id, @RequestBody AiFeedbackRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiConsultationService.saveFeedback(id, request)));
    }

    @GetMapping("/{id}/recommended-departments")
    public ResponseEntity<ApiResponse<List<RecommendedDepartmentDTO>>> recommendedDepartments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiConsultationService.getRecommendedDepartments(id)));
    }
}
