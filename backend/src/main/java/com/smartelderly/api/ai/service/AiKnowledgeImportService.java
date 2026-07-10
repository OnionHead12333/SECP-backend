package com.smartelderly.api.ai.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.smartelderly.api.ApiException;
import com.smartelderly.api.ai.dto.AiKnowledgeImportJobResponse;
import com.smartelderly.api.ai.dto.AiKnowledgeImportRequest;
import com.smartelderly.api.ai.dto.AiKnowledgeImportResponse;
import com.smartelderly.domain.ai.AiKnowledgeImportJob;
import com.smartelderly.domain.ai.AiKnowledgeImportJobRepository;

@Service
public class AiKnowledgeImportService {

    private final AiKnowledgeImportJobRepository jobRepository;
    private final AiKnowledgeImportWorker aiKnowledgeImportWorker;

    public AiKnowledgeImportService(AiKnowledgeImportJobRepository jobRepository,
            AiKnowledgeImportWorker aiKnowledgeImportWorker) {
        this.jobRepository = jobRepository;
        this.aiKnowledgeImportWorker = aiKnowledgeImportWorker;
    }

    @Transactional
    public AiKnowledgeImportResponse startImport(AiKnowledgeImportRequest request) {
        String filePath = request.getFilePath() == null ? "data/medical_qa_dataset.csv" : request.getFilePath();
        Path path = Path.of(filePath);
        if (!Files.exists(path) && !Path.of(filePath).isAbsolute()) {
            Path[] candidates = new Path[] {
                    Path.of(".").toAbsolutePath().normalize().resolve(filePath),
                    Path.of("backend").toAbsolutePath().normalize().resolve(filePath),
                    Path.of("D:/Desktop/SECP/SCEP-backend/backend").resolve(filePath)
            };
            boolean found = false;
            for (Path candidate : candidates) {
                if (Files.exists(candidate)) {
                    path = candidate;
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ApiException(4004, "导入文件不存在: " + filePath);
            }
        }

        AiKnowledgeImportJob job = new AiKnowledgeImportJob();
        job.setFileName(path.getFileName().toString());
        job.setFilePath(path.toString());
        job.setImportStatus("running");
        AiKnowledgeImportJob saved = jobRepository.saveAndFlush(job);
        Long jobId = saved.getId();
        aiKnowledgeImportWorker.importFromFileAsync(jobId);

        return AiKnowledgeImportResponse.builder()
                .jobId(saved.getId())
                .fileName(saved.getFileName())
                .totalRows(0)
                .successRows(0)
                .failedRows(0)
                .duplicateRows(0)
                .importStatus("running")
                .build();
    }

    @Transactional(readOnly = true)
    public AiKnowledgeImportJobResponse getJob(Long jobId) {
        AiKnowledgeImportJob job = jobRepository.findById(jobId).orElseThrow(() -> new ApiException(404, "导入任务不存在"));
        return AiKnowledgeImportJobResponse.builder()
                .id(job.getId())
                .fileName(job.getFileName())
                .filePath(job.getFilePath())
                .importStatus(job.getImportStatus())
                .totalRows(job.getTotalRows())
                .successRows(job.getSuccessRows())
                .failedRows(job.getFailedRows())
                .errorMessage(job.getErrorMessage())
                .build();
    }
}
