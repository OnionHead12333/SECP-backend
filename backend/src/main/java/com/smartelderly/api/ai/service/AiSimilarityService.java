package com.smartelderly.api.ai.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.MatchedQaDTO;
import com.smartelderly.domain.ai.AiMedicalQaKnowledge;

@Service
public class AiSimilarityService {

    public List<MatchedQaDTO> search(String inputText, List<AiMedicalQaKnowledge> knowledgeList, int topK) {
        String normalized = normalize(inputText);
        List<MatchedQaDTO> result = new ArrayList<>();
        for (AiMedicalQaKnowledge knowledge : knowledgeList) {
            double score = score(normalized, knowledge);
            if (score >= 0.35d) {
                result.add(MatchedQaDTO.builder()
                        .question(knowledge.getQuestion())
                        .answer(truncateAnswer(knowledge.getAnswer()))
                        .similarity(score)
                        .build());
            }
        }
        return result.stream()
                .sorted(Comparator.comparing(MatchedQaDTO::getSimilarity, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(m -> m.getQuestion() == null ? "" : m.getQuestion()))
                .limit(Math.max(topK, 1))
                .toList();
    }

    public double highestSimilarity(List<MatchedQaDTO> list) {
        return list == null || list.isEmpty() || list.get(0).getSimilarity() == null ? 0.0d : list.get(0).getSimilarity();
    }

    private double score(String inputText, AiMedicalQaKnowledge knowledge) {
        if (inputText == null || knowledge == null) return 0.0d;
        double score = 0.0d;
        String question = normalize(knowledge.getQuestion());
        String keywords = normalize(knowledge.getQuestionKeywords());
        String summary = normalize(knowledge.getAnswerSummary());
        String answer = normalize(knowledge.getAnswer());
        String symptomTag = normalize(knowledge.getSymptomTag());
        String diseaseTag = normalize(knowledge.getDiseaseTag());

        score += overlapScore(inputText, question, 0.42d);
        score += overlapScore(inputText, keywords, 0.18d);
        score += overlapScore(inputText, summary, 0.12d);
        score += overlapScore(inputText, answer, 0.08d);
        score += overlapScore(inputText, symptomTag, 0.18d);
        score += overlapScore(inputText, diseaseTag, 0.12d);

        return Math.min(score, 0.99d);
    }

    private double overlapScore(String inputText, String text, double weight) {
        if (text == null || text.isBlank()) return 0.0d;
        double score = 0.0d;
        String[] tokens = text.split("[，,;；\\s/、()（）]+|");
        for (String token : tokens) {
            if (token == null || token.isBlank()) continue;
            if (token.length() >= 2 && inputText.contains(token)) {
                score += weight;
            }
        }
        return score;
    }

    private String truncateAnswer(String answer) {
        if (answer == null) return null;
        String cleaned = answer.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= 120 ? cleaned : cleaned.substring(0, 120) + "...";
    }

    private String normalize(String value) {
        if (value == null) return null;
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
