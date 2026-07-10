package com.smartelderly.api;

import java.io.IOException;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.medical.CreateMedicalArchiveFolderRequest;
import com.smartelderly.api.dto.medical.MedicalArchiveFolderViewDto;
import com.smartelderly.api.dto.medical.MedicalDocumentDetailDto;
import com.smartelderly.api.dto.medical.MedicalDocumentSummaryDto;
import com.smartelderly.api.dto.medical.PatchMedicalDocumentRequest;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.medical.MedicalArchiveService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/medical")
public class MedicalArchiveRestController {

    private final MedicalArchiveService medicalArchiveService;

    public MedicalArchiveRestController(MedicalArchiveService medicalArchiveService) {
        this.medicalArchiveService = medicalArchiveService;
    }

    @GetMapping("/folders")
    public ApiResponse<java.util.List<MedicalArchiveFolderViewDto>> listFolders(
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId) {
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(medicalArchiveService.listFolders(principal, elderProfileId));
    }

    @PostMapping("/folders")
    public ApiResponse<MedicalArchiveFolderViewDto> createFolder(
            @Valid @RequestBody CreateMedicalArchiveFolderRequest request) {
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(medicalArchiveService.createFolder(principal, request));
    }

    @GetMapping("/documents")
    public ApiResponse<java.util.List<MedicalDocumentSummaryDto>> listDocuments(
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "docCategory", required = false) String docCategory) {
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(
                medicalArchiveService.listDocuments(principal, elderProfileId, folderId, docCategory));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<MedicalDocumentDetailDto> documentDetail(
            @PathVariable("id") long id,
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId) {
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(medicalArchiveService.getDocument(principal, elderProfileId, id));
    }

    @PatchMapping("/documents/{id}")
    public ApiResponse<MedicalDocumentDetailDto> patchDocument(
            @PathVariable("id") long id,
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId,
            @Valid @RequestBody PatchMedicalDocumentRequest request) {
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(medicalArchiveService.patchDocument(principal, elderProfileId, id, request));
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable("id") long id,
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId) {
        var principal = SecurityUtils.requireAuth();
        medicalArchiveService.deleteDocument(principal, elderProfileId, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<org.springframework.core.io.Resource> documentFile(
            @PathVariable("id") long id,
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId)
            throws IOException {
        var principal = SecurityUtils.requireAuth();
        MedicalArchiveService.FilePayload payload =
                medicalArchiveService.loadDocumentFile(principal, elderProfileId, id);
        return ResponseEntity.ok()
                .contentType(payload.mediaType())
                .body(payload.resource());
    }
}
