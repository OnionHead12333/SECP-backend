package com.smartelderly.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {

    Optional<EmergencyAlert> findFirstByElderProfileIdAndStatusOrderByIdDesc(
            Long elderProfileId,
            AlertStatus status);

    Optional<EmergencyAlert> findByIdAndElderProfileId(Long id, Long elderProfileId);

    Page<EmergencyAlert> findByElderProfileIdInAndStatusIn(
            Collection<Long> elderProfileIds,
            Collection<AlertStatus> statuses,
            Pageable pageable);

    @Modifying
    @Query(
            value = "UPDATE emergency_alerts SET status = 'sent', sent_time = :now "
                    + "WHERE status = 'pending_revoke' AND revoke_deadline IS NOT NULL AND revoke_deadline <= :now",
            nativeQuery = true)
    int bulkAutoSendPastDeadline(@Param("now") LocalDateTime now);
}
