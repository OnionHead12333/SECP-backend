package com.smartelderly.domain.community;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "community_demo_peer_profiles")
public class CommunityDemoPeerProfile {

    @Id
    @Column(name = "scope_key", length = 64)
    private String scopeKey;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "hint", length = 255)
    private String hint;

    @Column(name = "emoji", length = 16)
    private String emoji;

    @Column(name = "linked_elder_profile_id")
    private Long linkedElderProfileId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}