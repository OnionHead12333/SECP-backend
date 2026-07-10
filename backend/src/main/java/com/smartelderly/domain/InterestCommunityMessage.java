package com.smartelderly.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "interest_community_messages")
public class InterestCommunityMessage {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "community_id", nullable = false, length = 32)
    private String communityId;

    @Column(name = "sender_scope_key", nullable = false, length = 64)
    private String senderScopeKey;

    @Column(name = "sender_elder_profile_id")
    private Long senderElderProfileId;

    @Column(name = "sender_display_name", nullable = false, length = 64)
    private String senderDisplayName;

    @Column(name = "sender_role", nullable = false, length = 16)
    private String senderRole;

    @Column(name = "message_kind", nullable = false, length = 16)
    private String messageKind;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "audio_url", length = 512)
    private String audioUrl;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (durationMs == null) {
            durationMs = 0;
        }
    }
}
