package com.smartelderly.api.ai.service;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import com.smartelderly.api.ApiException;
import com.smartelderly.domain.ai.AiKnowledgeImportJob;
import com.smartelderly.domain.ai.AiKnowledgeImportJobRepository;
import com.smartelderly.domain.ai.AiMedicalQaKnowledge;
import com.smartelderly.domain.ai.AiMedicalQaKnowledgeRepository;

@Slf4j
@Service
public class AiKnowledgeImportWorker {

    private final AiMedicalQaKnowledgeRepository knowledgeRepository;
    private final AiKnowledgeImportJobRepository jobRepository;
    private final AiTextCleanService textCleanService;

    public AiKnowledgeImportWorker(AiMedicalQaKnowledgeRepository knowledgeRepository,
            AiKnowledgeImportJobRepository jobRepository,
            AiTextCleanService textCleanService) {
        this.knowledgeRepository = knowledgeRepository;
        this.jobRepository = jobRepository;
        this.textCleanService = textCleanService;
    }

    @Async
    public void importFromFileAsync(Long jobId) {
        AiKnowledgeImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(404, "导入任务不存在"));
        Path path = resolveFilePath(job.getFilePath());
        int total = 0, success = 0, failed = 0, duplicate = 0;
        Set<String> seen = new HashSet<>();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) {
                throw new ApiException(4004, "导入文件为空");
            }
            validateHeader(header);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                total++;
                try {
                    ParsedRow row = parseRow(line);

                    if (isBlank(row.question) || isBlank(row.answer) || textCleanService.isTooShort(row.question)) {
                        failed++;
                        updateJobProgress(jobId, total, success, failed, duplicate, "running", "invalid row");
                        continue;
                    }

                    String key = textCleanService.normalizeQuestionKey(row.question);
                    if (!seen.add(key) || knowledgeRepository.findFirstByQuestionIgnoreCase(row.question).isPresent()) {
                        duplicate++;
                        updateJobProgress(jobId, total, success, failed, duplicate, "running", null);
                        continue;
                    }

                    AiMedicalQaKnowledge entity = new AiMedicalQaKnowledge();
                    entity.setQuestion(row.question);
                    entity.setAnswer(row.answer);
                    entity.setQuestionKeywords(trimToLength(row.questionSeg, 1000));
                    entity.setAnswerSummary(trimToLength(row.answerSeg, 1000));
                    entity.setSceneTag(trimToLength(row.sceneFlag, 100));
                    entity.setDiseaseTag(trimToLength(row.conditionFlag, 200));
                    entity.setSymptomTag(trimToLength(row.conditionFlag, 500));
                    entity.setStatus("active");
                    entity.setRiskLevel("low");
                    entity.setSource("import_dataset");
                    knowledgeRepository.save(entity);
                    success++;
                    updateJobProgress(jobId, total, success, failed, duplicate, "running", null);
                } catch (Exception rowEx) {
                    failed++;
                    updateJobProgress(jobId, total, success, failed, duplicate, "running", rowEx.getMessage());
                    log.warn("Skip invalid row while importing ai knowledge, jobId={}, rowNo={}, error={}", jobId, total, rowEx.getMessage());
                }
            }
            updateJobProgress(jobId, total, success, failed, duplicate, "success", null);
        } catch (Exception e) {
            updateJobProgress(jobId, total, success, failed + 1, duplicate, "failed", e.getMessage());
            log.error("AI knowledge import failed, jobId={}", jobId, e);
        }
    }

    @Transactional
    public AiKnowledgeImportJob updateJobProgress(Long jobId, int total, int success, int failed, int duplicate, String status, String errorMessage) {
        AiKnowledgeImportJob current = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(404, "导入任务不存在"));
        current.setTotalRows(total);
        current.setSuccessRows(success);
        current.setFailedRows(failed);
        current.setImportStatus(status);
        current.setErrorMessage(errorMessage);
        return jobRepository.save(current);
    }

    private void validateHeader(String header) {
        String normalized = header.replace("\uFEFF", "").trim();
        if (!normalized.startsWith("question,answer,question_seg,answer_seg,_matched_elderly_scene,_matched_geriatric_condition")) {
            throw new ApiException(4004, "导入文件表头不符合预期");
        }
    }

    private ParsedRow parseRow(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            throw new ApiException(4004, "invalid row");
        }

        String question = cleanCsvToken(parts[0]);
        String sceneFlag = cleanCsvToken(parts[parts.length - 2]);
        String conditionFlag = cleanCsvToken(parts[parts.length - 1]);
        String questionSeg = cleanCsvToken(parts[parts.length - 4]);
        String answerSeg = cleanCsvToken(parts[parts.length - 3]);

        StringBuilder answer = new StringBuilder();
        for (int i = 1; i <= parts.length - 5; i++) {
            if (i > 1) {
                answer.append(',');
            }
            answer.append(parts[i]);
        }

        return new ParsedRow(
                textCleanService.cleanText(question),
                textCleanService.cleanText(answer.toString()),
                textCleanService.cleanText(questionSeg),
                textCleanService.cleanText(answerSeg),
                textCleanService.cleanText(sceneFlag),
                textCleanService.cleanText(conditionFlag));
    }

    private String cleanCsvToken(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.replace("\"\"", "\"").trim();
    }

    private Path resolveFilePath(String filePath) {
        Path path = Path.of(filePath);
        if (path.isAbsolute()) {
            return path;
        }
        Path[] candidates = new Path[] {
                Path.of(".").toAbsolutePath().normalize().resolve(filePath),
                Path.of("backend").toAbsolutePath().normalize().resolve(filePath),
                Path.of("D:/Desktop/SECP/SCEP-backend/backend").resolve(filePath)
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return path.toAbsolutePath().normalize();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ParsedRow(String question, String answer, String questionSeg, String answerSeg, String sceneFlag, String conditionFlag) {}
}
