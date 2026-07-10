package com.smartelderly.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    
    // 根据老人ID和状态查询最新的告警记录（用于首页显示）
    Optional<EmergencyAlert> findFirstByElderProfileIdAndStatusOrderByIdDesc(
            Long elderProfileId,
            AlertStatus status);

    // 根据ID和老人ID查询告警记录，确保只能访问自己的告警
    Optional<EmergencyAlert> findByIdAndElderProfileId(Long id, Long elderProfileId);

    // 根据老人ID列表和状态列表查询告警记录，支持分页
    Page<EmergencyAlert> findByElderProfileIdInAndStatusIn(
            Collection<Long> elderProfileIds,
            Collection<AlertStatus> statuses,
            Pageable pageable);

    
    //查询未活动提醒列表
    List<EmergencyAlert> findByElderProfileIdAndAlertTypeAndTriggerModeOrderByTriggerTimeDesc(
            Long elderProfileId,
            AlertType alertType,
            TriggerMode triggerMode);

    /**
     * 查询指定时间之后生成的指定类型提醒（用于检查最小间隔）
     */
    List<EmergencyAlert> findByElderProfileIdAndAlertTypeAndTriggerModeAndTriggerTimeAfter(
            Long elderProfileId,
            AlertType alertType,
            TriggerMode triggerMode,
            LocalDateTime triggerTimeAfter);

    @Modifying
    @Query(
            value = "UPDATE emergency_alerts SET status = 'sent', sent_time = :now "
                    + "WHERE status = 'pending_revoke' AND revoke_deadline IS NOT NULL AND revoke_deadline <= :now",
            nativeQuery = true)
    int bulkAutoSendPastDeadline(@Param("now") LocalDateTime now);
}
