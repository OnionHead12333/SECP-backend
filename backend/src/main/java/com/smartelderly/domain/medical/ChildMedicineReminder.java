package com.smartelderly.domain.medical;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "medicine_reminders")
public class ChildMedicineReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "elder_profile_id", nullable = false)
    private Long elderProfileId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "medicine_name", nullable = false, length = 100)
    private String medicineName;

    @Column(length = 50)
    private String dosage;

    @Column(name = "frequency_rule", length = 100)
    private String frequencyRule;

    @Column(name = "repeat_rule", length = 100)
    private String repeatRule;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "related_event_id")
    private Long relatedEventId;

    @Column(name = "remind_time", nullable = false)
    private LocalDateTime remindTime;

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
}
