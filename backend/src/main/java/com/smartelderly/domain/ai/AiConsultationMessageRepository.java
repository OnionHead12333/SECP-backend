package com.smartelderly.domain.ai;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConsultationMessageRepository extends JpaRepository<AiConsultationMessage, Long> {
    List<AiConsultationMessage> findByConsultationIdOrderByCreatedAtAsc(Long consultationId);
}
