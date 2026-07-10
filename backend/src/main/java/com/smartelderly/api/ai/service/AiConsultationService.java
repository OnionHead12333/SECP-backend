package com.smartelderly.api.ai.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ai.dto.AiConsultationCreateRequest;
import com.smartelderly.api.ai.dto.AiConsultationDetailResponse;
import com.smartelderly.api.ai.dto.AiConsultationHistoryItemDTO;
import com.smartelderly.api.ai.dto.AiConsultationMessageRequest;
import com.smartelderly.api.ai.dto.AiConsultationResponse;
import com.smartelderly.api.ai.dto.AiFeedbackRequest;
import com.smartelderly.api.ai.dto.AiFeedbackResponse;
import com.smartelderly.api.ai.dto.AiNotifyFamilyResponse;
import com.smartelderly.api.ai.dto.MatchedQaDTO;
import com.smartelderly.api.ai.dto.RecommendedDepartmentDTO;
import com.smartelderly.domain.ai.AiConsultation;
import com.smartelderly.domain.ai.AiConsultationMessage;
import com.smartelderly.domain.ai.AiConsultationMessageRepository;
import com.smartelderly.domain.ai.AiConsultationRepository;
import com.smartelderly.domain.ai.AiFeedback;
import com.smartelderly.domain.ai.AiFeedbackRepository;
import com.smartelderly.domain.ai.AiFamilyNotification;
import com.smartelderly.domain.ai.AiFamilyNotificationRepository;
import com.smartelderly.domain.ai.AiManualHandoff;
import com.smartelderly.domain.ai.AiManualHandoffRepository;
import com.smartelderly.domain.ai.AiMedicalQaKnowledge;
import com.smartelderly.domain.ai.AiMedicalQaKnowledgeRepository;

@Service
public class AiConsultationService {

    private static final Logger log = LoggerFactory.getLogger(AiConsultationService.class);
    private static final String SAFETY_NOTICE = "本回答仅供健康咨询参考，不能替代医生诊断。";
    private static final double FOLLOW_UP_THRESHOLD = 0.35d;

    private final AiConsultationRepository consultationRepository;
    private final AiConsultationMessageRepository messageRepository;
    private final AiMedicalQaKnowledgeRepository knowledgeRepository;
    private final AiKnowledgeService aiKnowledgeService;
    private final AiRiskRuleService aiRiskRuleService;
    private final AiAnswerGenerateService aiAnswerGenerateService;
    private final AiDepartmentRecommendService aiDepartmentRecommendService;
    private final AiFamilyNotificationRepository familyNotificationRepository;
    private final AiFeedbackRepository feedbackRepository;
    private final AiManualHandoffRepository manualHandoffRepository;
    private final AiQueryUnderstandingService aiQueryUnderstandingService;
    private final AiModelPredictionService aiModelPredictionService;
    private final AiTextCleanService aiTextCleanService;

    public AiConsultationService(AiConsultationRepository consultationRepository,
            AiConsultationMessageRepository messageRepository,
            AiMedicalQaKnowledgeRepository knowledgeRepository,
            AiKnowledgeService aiKnowledgeService,
            AiRiskRuleService aiRiskRuleService,
            AiAnswerGenerateService aiAnswerGenerateService,
            AiDepartmentRecommendService aiDepartmentRecommendService,
            AiFamilyNotificationRepository familyNotificationRepository,
            AiFeedbackRepository feedbackRepository,
            AiManualHandoffRepository manualHandoffRepository,
            AiQueryUnderstandingService aiQueryUnderstandingService,
            AiModelPredictionService aiModelPredictionService,
            AiTextCleanService aiTextCleanService) {
        this.consultationRepository = consultationRepository;
        this.messageRepository = messageRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.aiKnowledgeService = aiKnowledgeService;
        this.aiRiskRuleService = aiRiskRuleService;
        this.aiAnswerGenerateService = aiAnswerGenerateService;
        this.aiDepartmentRecommendService = aiDepartmentRecommendService;
        this.familyNotificationRepository = familyNotificationRepository;
        this.feedbackRepository = feedbackRepository;
        this.manualHandoffRepository = manualHandoffRepository;
        this.aiQueryUnderstandingService = aiQueryUnderstandingService;
        this.aiModelPredictionService = aiModelPredictionService;
        this.aiTextCleanService = aiTextCleanService;
    }

    @Transactional
    public AiConsultationResponse createConsultation(AiConsultationCreateRequest request) {
        log.info("[AI-入口] inputText={}, length={}, bytes={}", request.getInputText(),
                request.getInputText() == null ? 0 : request.getInputText().length(),
                request.getInputText() == null ? "null" : java.util.Arrays.toString(request.getInputText().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String rawText = aiTextCleanService.cleanText(request.getInputText());
        String normalized = normalize(rawText);
        AiConsultation consultation = new AiConsultation();
        consultation.setUserId(1L);
        consultation.setElderlyId(request.getElderlyId());
        consultation.setInputText(rawText == null ? request.getInputText().trim() : rawText);
        consultation.setInputType(request.getInputType());
        consultation.setNormalizedText(normalized);
        consultation.setStatus("processing");
        consultation = consultationRepository.save(consultation);

        saveMessage(consultation.getId(), "user", consultation.getInputText(), request.getInputType());
        return processConsultation(consultation, normalized, consultation.getInputText(), false);
    }

    @Transactional
    public AiConsultationResponse addMessage(Long id, AiConsultationMessageRequest request) {
        AiConsultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("咨询记录不存在"));
        saveMessage(id, "user", request.getMessageContent().trim(), request.getMessageType());
        String context = buildContext(id, consultation.getInputText(), request.getMessageContent().trim());
        consultation.setNormalizedText(normalize(context));
        consultationRepository.save(consultation);
        return processConsultation(consultation, consultation.getNormalizedText(), context, true);
    }

    @Transactional(readOnly = true)
    public AiConsultationDetailResponse getConsultationDetail(Long id) {
        AiConsultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("咨询记录不存在"));
        List<AiConsultationMessage> messages = messageRepository.findByConsultationIdOrderByCreatedAtAsc(id);
        return AiConsultationDetailResponse.builder()
                .consultationId(consultation.getId())
                .elderlyId(consultation.getElderlyId())
                .inputText(consultation.getInputText())
                .inputType(consultation.getInputType())
                .riskLevel(consultation.getRiskLevel())
                .needMedicalVisit(Boolean.TRUE.equals(consultation.getNeedMedicalVisit()))
                .needFamilyNotify(Boolean.TRUE.equals(consultation.getNeedFamilyNotify()))
                .finalAnswer(consultation.getFinalAnswer())
                .recommendedDepartments(resolveRecommendedDepartments(consultation, contextMatchedList(consultation.getNormalizedText())))
                .matchedQaList(filterMatchedQa(consultation.getNormalizedText(), aiQueryUnderstandingService.understand(consultation.getNormalizedText())))
                .followUpQuestion(consultation.getFollowUpQuestion())
                .safetyNotice(consultation.getSafetyNotice())
                .status(consultation.getStatus())
                .createdAt(consultation.getCreatedAt() == null ? null : consultation.getCreatedAt().toString())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AiConsultationHistoryItemDTO> listConsultations(Long elderlyId, Integer page, Integer pageSize) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
        return consultationRepository.findByElderlyIdOrderByCreatedAtDesc(elderlyId, pageable)
                .map(c -> AiConsultationHistoryItemDTO.builder()
                        .consultationId(c.getId())
                        .createdAt(c.getCreatedAt() == null ? null : c.getCreatedAt().toString())
                        .inputText(c.getInputText())
                        .riskLevel(c.getRiskLevel())
                        .needMedicalVisit(c.getNeedMedicalVisit())
                        .needFamilyNotify(c.getNeedFamilyNotify())
                        .recommendedDepartments(resolveRecommendedDepartments(c, contextMatchedList(c.getNormalizedText())))
                        .finalAnswerSummary(summarize(c.getFinalAnswer()))
                        .status(c.getStatus())
                        .build())
                .getContent();
    }

    @Transactional
    public AiNotifyFamilyResponse notifyFamily(Long id) {
        AiConsultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("咨询记录不存在"));
        AiFamilyNotification n = new AiFamilyNotification();
        n.setConsultationId(id);
        n.setElderlyId(consultation.getElderlyId());
        n.setFamilyUserId(1L);
        n.setNotificationType("consultation");
        n.setContent(buildFamilyContent(consultation));
        familyNotificationRepository.save(n);
        return AiNotifyFamilyResponse.builder().success(true).message("已同步给家属").build();
    }

    @Transactional
    public AiFeedbackResponse saveFeedback(Long id, AiFeedbackRequest request) {
        AiFeedback feedback = new AiFeedback();
        feedback.setConsultationId(id);
        feedback.setUserId(1L);
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setFeedbackText(request.getFeedbackText());
        feedback.setIsHelpful(request.getIsHelpful());
        feedback.setHasVisitedDoctor(request.getHasVisitedDoctor());
        feedbackRepository.save(feedback);
        if ("need_manual_help".equalsIgnoreCase(request.getFeedbackType())) {
            AiManualHandoff handoff = new AiManualHandoff();
            handoff.setConsultationId(id);
            handoff.setReason(request.getFeedbackText());
            manualHandoffRepository.save(handoff);
        }
        return AiFeedbackResponse.builder().success(true).message("反馈已提交").build();
    }

    public List<RecommendedDepartmentDTO> getRecommendedDepartments(Long id) {
        AiConsultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("咨询记录不存在"));
        return resolveRecommendedDepartments(consultation, contextMatchedList(consultation.getNormalizedText()));
    }

    private AiConsultationResponse processConsultation(AiConsultation consultation, String normalized, String rawText, boolean isFollowUp) {
        String hardDetectedSymptomType = detectObviousSymptomType(rawText);
        var understanding = aiQueryUnderstandingService.understand(rawText);
        String queryUnderstandingSymptomType = understanding == null ? "unknown" : understanding.symptomType();
        if (hardDetectedSymptomType != null && !"unknown".equalsIgnoreCase(hardDetectedSymptomType)
                && (understanding == null || "unknown".equalsIgnoreCase(queryUnderstandingSymptomType))) {
            understanding = mergeUnderstanding(rawText, understanding, hardDetectedSymptomType);
        }
        String finalSymptomType = understanding == null ? "unknown" : understanding.symptomType();
        var highRisk = aiRiskRuleService.decide(normalized);
        AiModelPredictionService.AiModelPredictionResult modelResult = safePredict(rawText);
        List<MatchedQaDTO> matched = filterMatchedQa(normalized, understanding);
        List<RecommendedDepartmentDTO> departments = buildDepartments(understanding, highRisk, modelResult, matched);

        boolean dangerHit = !highRisk.recommendedDepartments().isEmpty() || (understanding != null && !understanding.dangerSignals().isEmpty());
        String branchName;
        if (dangerHit) {
            branchName = "high_risk";
            consultation.setRiskLevel(normalizeRisk(highRisk.riskLevel(), modelResult, true));
            consultation.setNeedMedicalVisit(true);
            consultation.setNeedFamilyNotify(true);
            List<RecommendedDepartmentDTO> emergencyDepts = departments.isEmpty() ? highRisk.recommendedDepartments() : departments;
            consultation.setFinalAnswer(aiAnswerGenerateService.generateHighRiskAnswer(emergencyDepts));
            consultation.setFollowUpQuestion(null);
            consultation.setSafetyNotice(SAFETY_NOTICE);
            consultation.setRecommendedDepartmentName(joinDepartmentNames(emergencyDepts));
            consultation.setStatus("done");
            consultation = consultationRepository.save(consultation);
            log.info("[AI] branch=high_risk | originalText={} | symptomType={} | departments={}", rawText, finalSymptomType, joinDepartmentNames(emergencyDepts));
            saveMessage(consultation.getId(), "assistant", consultation.getFinalAnswer(), "high_risk_answer");
            return buildResponse(consultation, finalSymptomType, List.of(), emergencyDepts);
        }

        double topSimilarity = matched.isEmpty() ? 0d : matched.get(0).getSimilarity() == null ? 0d : matched.get(0).getSimilarity();
        boolean unknown = "unknown".equalsIgnoreCase(finalSymptomType);
        boolean needFollowUp = understanding != null && (understanding.needFollowUp() || topSimilarity < FOLLOW_UP_THRESHOLD);
        boolean modelStrong = modelResult != null && (modelResult.riskConfidence() >= 0.65d || modelResult.departmentConfidence() >= 0.6d);

        String finalRisk = normalizeRisk(highRisk.riskLevel(), modelResult, false);
        consultation.setRiskLevel(finalRisk);
        consultation.setNeedMedicalVisit("high".equalsIgnoreCase(finalRisk) || "emergency".equalsIgnoreCase(finalRisk));
        consultation.setNeedFamilyNotify(Boolean.TRUE.equals(consultation.getNeedMedicalVisit()));

        if (!unknown) {
            branchName = needFollowUp ? "symptom_follow_up" : "general_answer";
            consultation.setStatus(needFollowUp ? "need_more_info" : "done");
            consultation.setFinalAnswer(aiAnswerGenerateService.generateSymptomFollowUpAnswer(rawText, finalSymptomType, finalRisk, matched, departments,
                    needFollowUp, understanding.followUpQuestions(), understanding.dangerSignals()));
            consultation.setFollowUpQuestion(firstFollowUpQuestion(understanding.followUpQuestions()));
            consultation.setSafetyNotice(SAFETY_NOTICE);
            consultation.setRecommendedDepartmentName(joinDepartmentNames(departments));
            consultation = consultationRepository.save(consultation);
            log.info("[AI] branch={} | originalText={} | symptomType={} | needFollowUp={} | riskLevel={} | departments={}",
                    branchName, rawText, finalSymptomType, needFollowUp, finalRisk, joinDepartmentNames(departments));
            saveMessage(consultation.getId(), "assistant", consultation.getFinalAnswer(), "normal_answer");
            return buildResponse(consultation, finalSymptomType, matched, departments);
        }

        branchName = "unknown_follow_up";
        consultation.setStatus("need_more_info");
        consultation.setFinalAnswer(aiAnswerGenerateService.generateUnknownFollowUpAnswer(understanding == null ? List.of() : understanding.followUpQuestions()));
        consultation.setFollowUpQuestion(firstFollowUpQuestion(understanding == null ? List.of() : understanding.followUpQuestions()));
        consultation.setSafetyNotice(SAFETY_NOTICE);
        consultation.setRecommendedDepartmentName(joinDepartmentNames(departments));
        consultation = consultationRepository.save(consultation);
        log.info("[AI] branch=unknown_follow_up | originalText={} | symptomType={} | riskLevel={} | departments={}",
                rawText, finalSymptomType, finalRisk, joinDepartmentNames(departments));
        saveMessage(consultation.getId(), "assistant", consultation.getFollowUpQuestion(), "follow_up_question");
        return buildResponse(consultation, finalSymptomType, matched, departments);
    }

    private AiConsultationResponse buildResponse(AiConsultation consultation, String symptomType, List<MatchedQaDTO> matched, List<RecommendedDepartmentDTO> departments) {
        return AiConsultationResponse.builder()
                .consultationId(consultation.getId())
                .symptomType(symptomType)
                .riskLevel(consultation.getRiskLevel())
                .needMedicalVisit(consultation.getNeedMedicalVisit())
                .needFamilyNotify(consultation.getNeedFamilyNotify())
                .finalAnswer(consultation.getFinalAnswer())
                .recommendedDepartments(departments)
                .matchedQaList(matched)
                .followUpQuestion(consultation.getFollowUpQuestion())
                .safetyNotice(consultation.getSafetyNotice())
                .status(consultation.getStatus())
                .build();
    }

    private void saveMessage(Long consultationId, String senderType, String content, String messageType) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        AiConsultationMessage msg = new AiConsultationMessage();
        msg.setConsultationId(consultationId);
        msg.setSenderType(senderType);
        msg.setMessageContent(content);
        msg.setMessageType(messageType);
        messageRepository.save(msg);
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private String buildContext(Long consultationId, String original, String message) {
        List<AiConsultationMessage> messages = messageRepository.findByConsultationIdOrderByCreatedAtAsc(consultationId);
        StringBuilder sb = new StringBuilder();
        if (original != null) sb.append(original);
        for (AiConsultationMessage m : messages) {
            if (m.getMessageContent() != null) sb.append(' ').append(m.getMessageContent());
        }
        if (message != null) sb.append(' ').append(message);
        return sb.toString();
    }

    private List<MatchedQaDTO> filterMatchedQa(String normalized, AiQueryUnderstandingService.QueryUnderstandingResult understanding) {
        List<MatchedQaDTO> candidates = aiKnowledgeService.searchTopK(normalized, 3);
        if (candidates.isEmpty()) {
            return List.of();
        }
        double threshold = understanding != null && "unknown".equalsIgnoreCase(understanding.symptomType()) ? FOLLOW_UP_THRESHOLD : 0.5d;
        return candidates.stream()
                .filter(item -> item.getSimilarity() != null && item.getSimilarity() >= threshold)
                .limit(3)
                .toList();
    }

    private List<RecommendedDepartmentDTO> buildDepartments(AiQueryUnderstandingService.QueryUnderstandingResult understanding,
            AiRiskRuleService.RiskDecision highRisk, AiModelPredictionService.AiModelPredictionResult modelResult,
            List<MatchedQaDTO> matched) {
        List<RecommendedDepartmentDTO> departments = new java.util.ArrayList<>();
        if (highRisk != null && highRisk.recommendedDepartments() != null && !highRisk.recommendedDepartments().isEmpty()) {
            departments.addAll(highRisk.recommendedDepartments());
        }
        if (modelResult != null && modelResult.departmentConfidence() >= 0.6d && modelResult.departmentName() != null) {
            departments.add(RecommendedDepartmentDTO.builder()
                    .departmentId(1L)
                    .departmentName(modelResult.departmentName())
                    .reason("根据症状描述的科室推荐结果")
                    .build());
        }
        if (understanding != null) {
            departments.addAll(symptomDepartments(understanding.symptomType()));
        }
        if (departments.isEmpty()) {
            departments.addAll(defaultDepartments());
        }
        return departments.stream()
                .filter(d -> d != null && d.getDepartmentName() != null && !d.getDepartmentName().isBlank())
                .distinct()
                .limit(3)
                .toList();
    }

    private List<RecommendedDepartmentDTO> symptomDepartments(String symptomType) {
        if (symptomType == null) return List.of();
        return switch (symptomType) {
            case "dizziness" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("神经内科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("全科医学科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(3L).departmentName("内科").reason("根据症状描述的科室推荐结果").build());
            case "pain_fall" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("骨科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("全科医学科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(3L).departmentName("内科").reason("根据症状描述的科室推荐结果").build());
            case "insomnia_anxiety" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("神经内科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("精神心理科").reason("根据症状描述的科室推荐结果").build());
            case "chest" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("心血管内科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("急诊科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(3L).departmentName("呼吸内科").reason("根据症状描述的科室推荐结果").build());
            case "dyspnea" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("呼吸内科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("急诊科").reason("根据症状描述的科室推荐结果").build());
            case "fever_cough" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("呼吸内科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("全科医学科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(3L).departmentName("感染科").reason("根据症状描述的科室推荐结果").build());
            case "abdominal" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("消化内科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("全科医学科").reason("根据症状描述的科室推荐结果").build());
            case "urinary" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("泌尿外科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("全科医学科").reason("根据症状描述的科室推荐结果").build());
            case "blood_stool" -> List.of(
                    RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("消化内科").reason("根据症状描述的科室推荐结果").build(),
                    RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("急诊科").reason("根据症状描述的科室推荐结果").build());
            default -> List.of();
        };
    }

    private List<RecommendedDepartmentDTO> defaultDepartments() {
        return List.of(
                RecommendedDepartmentDTO.builder().departmentId(1L).departmentName("全科医学科").reason("适合初步评估").build(),
                RecommendedDepartmentDTO.builder().departmentId(2L).departmentName("内科").reason("常见内科症状初筛").build(),
                RecommendedDepartmentDTO.builder().departmentId(3L).departmentName("导诊台").reason("不确定时先导诊").build());
    }

    private List<AiMedicalQaKnowledge> contextMatchedList(String normalized) {
        return knowledgeRepository.findByStatusOrderByIdAsc("active").stream().toList();
    }

    private List<RecommendedDepartmentDTO> resolveRecommendedDepartments(AiConsultation consultation, List<AiMedicalQaKnowledge> matchedKnowledge) {
        var risk = aiRiskRuleService.decide(consultation.getNormalizedText());
        return aiDepartmentRecommendService.recommendByRiskAndKnowledge(risk, matchedKnowledge);
    }

    private String summarize(String finalAnswer) {
        if (finalAnswer == null) return "";
        return finalAnswer.length() > 80 ? finalAnswer.substring(0, 80) + "..." : finalAnswer;
    }

    private String joinDepartmentNames(List<RecommendedDepartmentDTO> departments) {
        if (departments == null || departments.isEmpty()) return "全科医学科";
        return departments.stream().map(RecommendedDepartmentDTO::getDepartmentName).filter(v -> v != null && !v.isBlank()).distinct().limit(3).reduce((a, b) -> a + " / " + b).orElse("全科医学科");
    }

    private String firstFollowUpQuestion(List<String> questions) {
        return questions == null || questions.isEmpty() ? null : questions.get(0);
    }

    private AiQueryUnderstandingService.QueryUnderstandingResult mergeUnderstanding(String rawText,
            AiQueryUnderstandingService.QueryUnderstandingResult understanding, String hardDetectedSymptomType) {
        String text = rawText == null ? "" : rawText;
        if (understanding == null) {
            return new AiQueryUnderstandingService.QueryUnderstandingResult(
                    text,
                    normalize(text),
                    hardDetectedSymptomType,
                    0.95d,
                    List.of(hardDetectedSymptomType),
                    "advice",
                    0.9d,
                    List.of(),
                    false,
                    true,
                    defaultFollowUpQuestions(hardDetectedSymptomType),
                    null);
        }
        return new AiQueryUnderstandingService.QueryUnderstandingResult(
                text,
                normalize(text),
                hardDetectedSymptomType,
                0.95d,
                understanding.symptomKeywords() == null || understanding.symptomKeywords().isEmpty()
                        ? List.of(hardDetectedSymptomType)
                        : understanding.symptomKeywords(),
                understanding.intentType(),
                understanding.intentConfidence(),
                understanding.dangerSignals(),
                false,
                true,
                understanding.followUpQuestions(),
                understanding.unknownReason());
    }

    private List<String> defaultFollowUpQuestions(String symptomType) {
        if ("dizziness".equalsIgnoreCase(symptomType)) {
            return List.of("是突然晕，还是已经持续一段时间？", "是天旋地转，还是发飘发虚？", "有没有恶心、呕吐、耳鸣、胸闷或手脚无力？");
        }
        if ("pain_fall".equalsIgnoreCase(symptomType)) {
            return List.of("疼痛持续多久了？", "有没有摔倒、扭伤或突然加重？", "膝盖有没有红肿、发热或不能弯曲？");
        }
        if ("insomnia_anxiety".equalsIgnoreCase(symptomType)) {
            return List.of("睡不着是入睡困难还是容易醒？", "持续多久了？", "最近有没有心烦、紧张、情绪波动？");
        }
        if ("chest".equalsIgnoreCase(symptomType) || "dyspnea".equalsIgnoreCase(symptomType)) {
            return List.of("症状是突然出现还是逐渐加重？", "有没有胸痛、胸闷或喘不上气？", "有没有说话不清、出汗、心慌或手脚无力？");
        }
        return List.of("请补充症状的具体表现。", "请补充持续时间和伴随症状。");
    }

    private String detectObviousSymptomType(String text) {
        String t = text == null ? "" : text.trim();
        if (containsAny(t, "胸闷", "胸痛", "心慌", "心悸")) {
            return "chest";
        }
        if (containsAny(t, "呼吸困难", "喘不上气", "气短", "憋气")) {
            return "dyspnea";
        }
        if (containsAny(t, "睡不着", "失眠", "多梦", "睡眠不好", "焦虑", "心烦")) {
            return "insomnia_anxiety";
        }
        if (containsAny(t, "膝盖疼", "腿疼", "关节疼", "走路疼", "腰腿疼", "摔倒", "跌倒", "扭伤")) {
            return "pain_fall";
        }
        if (containsAny(t, "晕", "头晕", "眩晕", "发晕", "头昏", "天旋地转", "站不稳", "眼前发黑", "发飘")) {
            return "dizziness";
        }
        return "unknown";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private AiModelPredictionService.AiModelPredictionResult safePredict(String text) {
        try {
            return aiModelPredictionService.predictInsight(text);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeRisk(String ruleRisk, AiModelPredictionService.AiModelPredictionResult modelResult, boolean highPriority) {
        if (highPriority) {
            return (ruleRisk == null || ruleRisk.isBlank()) ? "high" : ruleRisk;
        }
        String risk = (ruleRisk == null || ruleRisk.isBlank()) ? "low" : ruleRisk;
        if (modelResult == null) {
            return risk;
        }
        String modelRisk = modelResult.riskLevel();
        double confidence = modelResult.riskConfidence();
        if ("high".equalsIgnoreCase(modelRisk) || "emergency".equalsIgnoreCase(modelRisk)) {
            return confidence >= 0.65d ? modelRisk : ("high".equalsIgnoreCase(risk) ? risk : "medium");
        }
        if ("medium".equalsIgnoreCase(modelRisk) && confidence >= 0.6d) {
            return "medium";
        }
        return risk;
    }

    private String buildFamilyContent(AiConsultation consultation) {
        return "老人问题：" + consultation.getInputText() + "\n" +
                "风险等级：" + consultation.getRiskLevel() + "\n" +
                "AI 回答摘要：" + summarize(consultation.getFinalAnswer()) + "\n" +
                "推荐科室：" + consultation.getRecommendedDepartmentName() + "\n" +
                "是否建议就医：" + consultation.getNeedMedicalVisit() + "\n" +
                "咨询时间：" + consultation.getCreatedAt();
    }
}
