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
@Table(name = "direct_message_threads")
public class DirectMessageThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_a_scope_key", nullable = false, length = 64)
    private String participantAScopeKey;

    @Column(name = "participant_b_scope_key", nullable = false, length = 64)
    private String participantBScopeKey;

    @Column(name = "participant_a_elder_profile_id")
    private Long participantAElderProfileId;

    @Column(name = "participant_b_elder_profile_id")
    private Long participantBElderProfileId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}