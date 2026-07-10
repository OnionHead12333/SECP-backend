package com.smartelderly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_execution_logs")
public class ReminderExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "elder_profile_id")
    private Long elderProfileId;
    @Column(name = "reminder_id")
    private Long reminderId;
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    private String status; // pending, confirmed, missed, snoozed, etc.
    private String reminderKind;
    private String confirmSource;
    private Boolean isTimeout;

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Long getElderProfileId() { return elderProfileId; }
    public void setElderProfileId(Long elderProfileId) { this.elderProfileId = elderProfileId; }

    public Long getReminderId() { return reminderId; }
    public void setReminderId(Long reminderId) { this.reminderId = reminderId; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReminderKind() { return reminderKind; }
    public void setReminderKind(String reminderKind) { this.reminderKind = reminderKind; }

    public String getConfirmSource() { return confirmSource; }
    public void setConfirmSource(String confirmSource) { this.confirmSource = confirmSource; }

    public Boolean getIsTimeout() { return isTimeout; }
    public void setIsTimeout(Boolean isTimeout) { this.isTimeout = isTimeout; }
}