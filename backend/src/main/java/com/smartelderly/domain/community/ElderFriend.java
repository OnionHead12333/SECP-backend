package com.smartelderly.domain.community;

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
@Table(name = "elder_friends")
public class ElderFriend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_elder_profile_id", nullable = false)
    private Long ownerElderProfileId;

    @Column(name = "owner_scope_key", nullable = false, length = 64)
    private String ownerScopeKey;

    @Column(name = "friend_scope_key", nullable = false, length = 64)
    private String friendScopeKey;

    @Column(name = "friend_elder_profile_id")
    private Long friendElderProfileId;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "hint", length = 255)
    private String hint;

    @Column(name = "emoji", length = 16)
    private String emoji;

    @Column(name = "added_at")
    private LocalDateTime addedAt;
}