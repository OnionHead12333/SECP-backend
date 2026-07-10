package com.smartelderly.domain.ai;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMedicalQaKnowledgeRepository extends JpaRepository<AiMedicalQaKnowledge, Long> {

    Optional<AiMedicalQaKnowledge> findFirstByQuestionIgnoreCase(String question);

    long countByStatus(String status);

    List<AiMedicalQaKnowledge> findByStatusOrderByIdAsc(String status);
}
