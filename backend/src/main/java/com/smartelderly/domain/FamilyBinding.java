package com.smartelderly.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "family_bindings")
public class FamilyBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "elder_profile_id", nullable = false)
    private Long elderProfileId;

    @Column(name = "child_user_id", nullable = false)
    private Long childUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private BindingStatus status = BindingStatus.pending;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getElderProfileId() {
        return elderProfileId;
    }

    public void setElderProfileId(Long elderProfileId) {
        this.elderProfileId = elderProfileId;
    }

    public Long getChildUserId() {
        return childUserId;
    }

    public void setChildUserId(Long childUserId) {
        this.childUserId = childUserId;
    }

    public BindingStatus getStatus() {
        return status;
    }

    public void setStatus(BindingStatus status) {
        this.status = status;
    }
}
