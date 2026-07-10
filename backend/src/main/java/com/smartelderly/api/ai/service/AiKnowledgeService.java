package com.smartelderly.api.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.MatchedQaDTO;
import com.smartelderly.domain.ai.AiMedicalQaKnowledge;
import com.smartelderly.domain.ai.AiMedicalQaKnowledgeRepository;

@Service
public class AiKnowledgeService {

    private final AiMedicalQaKnowledgeRepository repository;
    private final AiSimilarityService similarityService;

    public AiKnowledgeService(AiMedicalQaKnowledgeRepository repository, AiSimilarityService similarityService) {
        this.repository = repository;
        this.similarityService = similarityService;
    }

    public List<AiMedicalQaKnowledge> activeKnowledge() {
        return repository.findByStatusOrderByIdAsc("active");
    }

    public List<MatchedQaDTO> searchTopK(String inputText, int topK) {
        return similarityService.search(inputText, activeKnowledge(), topK);
    }

    public long countTotal() {
        return repository.count();
    }

    public long countActive() {
        return repository.countByStatus("active");
    }

    public long countDisabled() {
        return repository.countByStatus("inactive");
    }
}
