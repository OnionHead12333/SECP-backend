package com.smartelderly.api.ai.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ai.dto.AiTrainingJobResponse;
import com.smartelderly.api.ai.dto.AiTrainingRequest;
import com.smartelderly.api.ai.dto.AiTrainingResponse;
import com.smartelderly.api.ai.dto.AiTrainingStatusResponse;
import com.smartelderly.domain.ai.AiMedicalQaKnowledgeRepository;
import com.smartelderly.domain.ai.AiTrainingJob;
import com.smartelderly.domain.ai.AiTrainingJobRepository;

@Service
public class AiTrainingService {

    private final AiMedicalQaKnowledgeRepository knowledgeRepository;
    private final AiTrainingJobRepository trainingJobRepository;

    @Value("${app.ai.index-dir:data/ai-index}")
    private String indexDir;

    private volatile String lastAlgorithm = "tfidf";
    private volatile Integer lastTopK = 5;
    private volatile Double lastSimilarityThreshold = 0.35d;
    private volatile Long lastTrainedAtEpochMillis = null;
    private volatile Long trainedQaCount = 0L;
    private volatile String status = "idle";
    private volatile String indexPath = null;

    public AiTrainingService(AiMedicalQaKnowledgeRepository knowledgeRepository, AiTrainingJobRepository trainingJobRepository) {
        this.knowledgeRepository = knowledgeRepository;
        this.trainingJobRepository = trainingJobRepository;
    }

    @Transactional
    public AiTrainingResponse train(AiTrainingRequest request) {
        long validSamples = knowledgeRepository.countByStatus("active");
        if (validSamples <= 0) {
            throw new ApiException(5002, "知识库没有可用于训练的 active 数据");
        }

        AiTrainingJob job = new AiTrainingJob();
        job.setAlgorithm(request.getAlgorithm() == null ? "tfidf" : request.getAlgorithm());
        job.setStatus("running");
        job.setTotalSamples((int) knowledgeRepository.count());
        job.setValidSamples((int) validSamples);
        job.setDuplicateSamples(Math.max(0, job.getTotalSamples() - job.getValidSamples()));
        job.setStartedAt(LocalDateTime.now());
        job = trainingJobRepository.saveAndFlush(job);
        Long jobId = job.getId();

        runTraining(jobId, request);

        return AiTrainingResponse.builder()
                .jobId(jobId)
                .success(true)
                .message("训练已开始")
                .algorithm(job.getAlgorithm())
                .topK(request.getTopK() == null ? 5 : request.getTopK())
                .similarityThreshold(request.getSimilarityThreshold() == null ? 0.35d : request.getSimilarityThreshold())
                .trainedQaCount(validSamples)
                .trainedAtEpochMillis(System.currentTimeMillis())
                .build();
    }

    @Async
    public void runTraining(Long jobId, AiTrainingRequest request) {
        AiTrainingJob job = trainingJobRepository.findById(jobId).orElseThrow(() -> new ApiException(404, "训练任务不存在"));
        try {
            status = "training";
            Path dir = Path.of(indexDir);
            Files.createDirectories(dir);
            indexPath = dir.resolve("ai_medical_qa_index.txt").toString();

            List<String> lines = knowledgeRepository.findByStatusOrderByIdAsc("active").stream()
                    .map(item -> item.getId() + "\t" + safe(item.getQuestion()) + "\t" + safe(item.getQuestionKeywords()) + "\t" + safe(item.getAnswerSummary()))
                    .toList();
            Files.write(Path.of(indexPath), lines, StandardCharsets.UTF_8);

            status = "training_completed";
            lastAlgorithm = job.getAlgorithm();
            lastTopK = request.getTopK() == null ? 5 : request.getTopK();
            lastSimilarityThreshold = request.getSimilarityThreshold() == null ? 0.35d : request.getSimilarityThreshold();
            trainedQaCount = (long) lines.size();
            lastTrainedAtEpochMillis = System.currentTimeMillis();

            job.setStatus("success");
            job.setFinishedAt(LocalDateTime.now());
            job.setDurationMs(Duration.between(job.getStartedAt(), job.getFinishedAt()).toMillis());
            job.setIndexPath(indexPath);
            trainingJobRepository.save(job);
        } catch (Exception e) {
            job.setStatus("failed");
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            job.setDurationMs(Duration.between(job.getStartedAt(), job.getFinishedAt()).toMillis());
            trainingJobRepository.save(job);
            status = "failed";
            throw new ApiException(5002, "训练失败：" + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AiTrainingStatusResponse getStatus() {
        return AiTrainingStatusResponse.builder()
                .status(status)
                .message("训练状态查询成功")
                .algorithm(lastAlgorithm)
                .topK(lastTopK)
                .similarityThreshold(lastSimilarityThreshold)
                .trainedQaCount(trainedQaCount)
                .lastTrainedAtEpochMillis(lastTrainedAtEpochMillis)
                .build();
    }

    @Transactional(readOnly = true)
    public AiTrainingJobResponse getJob(Long jobId) {
        AiTrainingJob job = trainingJobRepository.findById(jobId).orElseThrow(() -> new ApiException(404, "训练任务不存在"));
        return AiTrainingJobResponse.builder()
                .id(job.getId())
                .algorithm(job.getAlgorithm())
                .status(job.getStatus())
                .totalSamples(job.getTotalSamples())
                .validSamples(job.getValidSamples())
                .duplicateSamples(job.getDuplicateSamples())
                .indexPath(job.getIndexPath())
                .errorMessage(job.getErrorMessage())
                .build();
    }

    public String getIndexPath() {
        return indexPath;
    }

    private String safe(String v) {
        return v == null ? "" : v.replace("\n", " ").replace("\r", " ").trim();
    }
}
