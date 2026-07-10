package com.smartelderly.domain.community;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "direct_messages")
public class DirectMessage {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

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

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}