package com.smartelderly.domain;

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
@Table(name = "interest_community_chat_clear")
public class InterestCommunityChatClear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viewer_scope_key", nullable = false, length = 64)
    private String viewerScopeKey;

    @Column(name = "viewer_user_id")
    private Long viewerUserId;

    @Column(name = "elder_profile_id", nullable = false)
    private Long elderProfileId;

    @Column(name = "community_id", nullable = false, length = 32)
    private String communityId;

    @Column(name = "clear_before_millis", nullable = false)
    private Long clearBeforeMillis;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
