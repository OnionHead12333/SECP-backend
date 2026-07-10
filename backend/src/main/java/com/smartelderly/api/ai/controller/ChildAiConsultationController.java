package com.smartelderly.api.ai.controller;

import java.util.List;

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
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.AuthPrincipal;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.medical.MedicalAccessService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/v1/child/ai/consultations")
public class ChildAiConsultationController {

    private final AiConsultationService aiConsultationService;
    private final MedicalAccessService medicalAccessService;

    public ChildAiConsultationController(
            AiConsultationService aiConsultationService,
            MedicalAccessService medicalAccessService) {
        this.aiConsultationService = aiConsultationService;
        this.medicalAccessService = medicalAccessService;
    }

    @PostMapping
    public ApiResponse<AiConsultationResponse> create(@Valid @RequestBody AiConsultationCreateRequest request) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        medicalAccessService.resolveTargetElderProfileId(principal, request.getElderlyId());
        return ApiResponse.ok("操作成功", aiConsultationService.createConsultation(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiConsultationDetailResponse> detail(@PathVariable Long id) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        AiConsultationDetailResponse detail = aiConsultationService.getConsultationDetail(id);
        medicalAccessService.resolveTargetElderProfileId(principal, detail.getElderlyId());
        return ApiResponse.ok("操作成功", detail);
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<AiConsultationResponse> addMessage(
            @PathVariable Long id,
            @Valid @RequestBody AiConsultationMessageRequest request) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        verifyConsultationAccess(principal, id);
        return ApiResponse.ok("操作成功", aiConsultationService.addMessage(id, request));
    }

    @GetMapping
    public ApiResponse<List<AiConsultationHistoryItemDTO>> history(
            @RequestParam Long elderlyId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        medicalAccessService.resolveTargetElderProfileId(principal, elderlyId);
        return ApiResponse.ok("操作成功", aiConsultationService.listConsultations(elderlyId, page, pageSize));
    }

    @PostMapping("/{id}/notify-family")
    public ApiResponse<AiNotifyFamilyResponse> notifyFamily(@PathVariable Long id) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        verifyConsultationAccess(principal, id);
        return ApiResponse.ok("操作成功", aiConsultationService.notifyFamily(id));
    }

    @PostMapping("/{id}/feedback")
    public ApiResponse<AiFeedbackResponse> feedback(
            @PathVariable Long id,
            @RequestBody AiFeedbackRequest request) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        verifyConsultationAccess(principal, id);
        return ApiResponse.ok("操作成功", aiConsultationService.saveFeedback(id, request));
    }

    @GetMapping("/{id}/recommended-departments")
    public ApiResponse<List<RecommendedDepartmentDTO>> recommendedDepartments(@PathVariable Long id) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        verifyConsultationAccess(principal, id);
        return ApiResponse.ok("操作成功", aiConsultationService.getRecommendedDepartments(id));
    }

    private void verifyConsultationAccess(AuthPrincipal principal, Long consultationId) {
        AiConsultationDetailResponse detail = aiConsultationService.getConsultationDetail(consultationId);
        medicalAccessService.resolveTargetElderProfileId(principal, detail.getElderlyId());
    }
}
