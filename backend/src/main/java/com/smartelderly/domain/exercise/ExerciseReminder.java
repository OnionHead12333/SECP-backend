package com.smartelderly.domain.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 锻炼提醒
 */
@Data
@Entity
@Table(name = "exercise_reminders")
public class ExerciseReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "elder_profile_id", nullable = false)
    private Long elderProfileId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "exercise_type", nullable = false, length = 50)
    private String exerciseType;

    @Column(name = "goal_value")
    private Integer goalValue;

    @Column(name = "goal_unit", length = 32)
    private String goalUnit;

    @Column(name = "interval_minutes")
    private Integer intervalMinutes;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "remind_time", nullable = false)
    private LocalDateTime remindTime;

    @Column(name = "repeat_rule", nullable = false, length = 100)
    private String repeatRule;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_by", nullable = false, length = 16)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (enabled == null) {
            enabled = true;
        }
        if (status == null) {
            status = "pending";
        }
        if (goalUnit == null) {
            goalUnit = "minutes";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
