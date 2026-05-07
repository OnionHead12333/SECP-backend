package com.smartelderly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_execution_logs")
public class ReminderExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    @Column(name = "elder_profile_id")
    public int elderId;
    @Column(name = "reminder_id")
    public int reminderId;
    @Column(name = "scheduled_at")
    public LocalDateTime scheduledAt;
    @Column(name = "confirmed_at")
    public LocalDateTime confirmedAt;
    public String status; // pending, confirmed, missed, snoozed, etc.
    // ... 其他字段
}