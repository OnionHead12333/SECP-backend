package com.smartelderly.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReminderExecutionLogRepository extends JpaRepository<ReminderExecutionLog, Integer> {
    List<ReminderExecutionLog> findByElderIdAndScheduledAtBetween(int elderId, java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<ReminderExecutionLog> findByReminderId(int reminderId);
}
