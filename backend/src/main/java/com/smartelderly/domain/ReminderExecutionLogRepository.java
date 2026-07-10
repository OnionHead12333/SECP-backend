package com.smartelderly.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReminderExecutionLogRepository extends JpaRepository<ReminderExecutionLog, Integer> {
    List<ReminderExecutionLog> findByElderProfileIdAndScheduledAtBetween(Long elderProfileId, java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<ReminderExecutionLog> findByReminderId(Long reminderId);

    ReminderExecutionLog findTopByElderProfileIdAndReminderKindAndStatusOrderByConfirmedAtDesc(Long elderProfileId, String reminderKind, String status);

    List<ReminderExecutionLog> findByReminderKindAndStatusAndScheduledAtBetween(
            String reminderKind, String status, java.time.LocalDateTime start, java.time.LocalDateTime end);

    List<ReminderExecutionLog> findByElderProfileIdAndReminderKindAndScheduledAtBetween(
            Long elderProfileId, String reminderKind, java.time.LocalDateTime start, java.time.LocalDateTime end);

    default java.util.List<ReminderExecutionLog> findPendingWaterLogsBefore(java.time.LocalDateTime deadline, java.time.LocalDateTime dayStart) {
        return findByReminderKindAndStatusAndScheduledAtBetween("water", "pending", dayStart, deadline);
    }

    default java.util.List<ReminderExecutionLog> findTodayWaterLogs(Long elderProfileId, java.time.LocalDateTime dayStart, java.time.LocalDateTime dayEnd) {
        return findByElderProfileIdAndReminderKindAndScheduledAtBetween(elderProfileId, "water", dayStart, dayEnd);
    }

    List<ReminderExecutionLog> findByReminderIdAndStatus(Long reminderId, String status);

    // 兼容旧接口，参数为 Integer
    default java.util.List<ReminderExecutionLog> findByElderIdAndScheduledAtBetween(Integer elderId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (elderId == null) return java.util.Collections.emptyList();
        return findByElderProfileIdAndScheduledAtBetween(Long.valueOf(elderId), start, end);
    }
}
