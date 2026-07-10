package com.smartelderly.domain.ai;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFamilyNotificationRepository extends JpaRepository<AiFamilyNotification, Long> {
    List<AiFamilyNotification> findByConsultationId(Long consultationId);
}
