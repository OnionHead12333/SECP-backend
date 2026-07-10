package com.smartelderly.domain.ai;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "ai_family_notification")
public class AiFamilyNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    @Column(name = "elderly_id", nullable = false)
    private Long elderlyId;

    @Column(name = "family_user_id", nullable = false)
    private Long familyUserId;

    @Column(name = "notification_type", length = 50)
    private String notificationType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "send_status", length = 20)
    private String sendStatus = "sent";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
