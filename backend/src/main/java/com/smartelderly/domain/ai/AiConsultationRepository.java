package com.smartelderly.domain.ai;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConsultationRepository extends JpaRepository<AiConsultation, Long> {
    Page<AiConsultation> findByElderlyIdOrderByCreatedAtDesc(Long elderlyId, Pageable pageable);
    List<AiConsultation> findByElderlyIdOrderByCreatedAtDesc(Long elderlyId);
}
