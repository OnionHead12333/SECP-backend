package com.smartelderly.api.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.ai.dto.AiKnowledgeImportRequest;
import com.smartelderly.api.ai.dto.AiKnowledgeImportResponse;
import com.smartelderly.api.ai.dto.AiKnowledgeImportJobResponse;
import com.smartelderly.api.ai.dto.AiKnowledgeImportJobResponse;
import com.smartelderly.api.ai.service.AiKnowledgeImportService;
import com.smartelderly.api.ai.service.AiKnowledgeService;

@RestController
@RequestMapping("/admin/ai/knowledge")
public class AiKnowledgeController {

    private final AiKnowledgeImportService aiKnowledgeImportService;
    private final AiKnowledgeService aiKnowledgeService;

    public AiKnowledgeController(AiKnowledgeImportService aiKnowledgeImportService, AiKnowledgeService aiKnowledgeService) {
        this.aiKnowledgeImportService = aiKnowledgeImportService;
        this.aiKnowledgeService = aiKnowledgeService;
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<AiKnowledgeImportResponse>> importKnowledge(@RequestBody AiKnowledgeImportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("知识库导入已开始", aiKnowledgeImportService.startImport(request)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Object>> count() {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", java.util.Map.of(
                "total", aiKnowledgeService.countTotal(),
                "activeCount", aiKnowledgeService.countActive(),
                "disabledCount", aiKnowledgeService.countDisabled())));
    }

    @GetMapping("/import/{jobId}")
    public ResponseEntity<ApiResponse<AiKnowledgeImportJobResponse>> getImportJob(@org.springframework.web.bind.annotation.PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.ok("操作成功", aiKnowledgeImportService.getJob(jobId)));
    }
}
