package com.smartelderly.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "elder_profiles")
public class ElderProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "claimed_user_id")
    private Long claimedUserId;

    @Column(name = "location_permission_foreground")
    private Boolean locationPermissionForeground;

    @Column(name = "location_permission_background")
    private Boolean locationPermissionBackground;

    @Column(name = "permission_updated_at")
    private LocalDateTime permissionUpdatedAt;

    @Column(name = "created_by_child_id")
    private Long createdByChildId;

    @Column(name = "status", length = 20)
    private String status; // unclaimed / claimed，与表 elder_profiles.status 一致

    @Column(name = "gender", length = 16)
    private String gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    /**
     * 与库表 NOT NULL 一致：Hibernate 若插入显式 null 会绕开列默认值，需在此补默认。
     */
    @PrePersist
    void applyPermissionDefaults() {
        if (locationPermissionForeground == null) {
            locationPermissionForeground = false;
        }
        if (locationPermissionBackground == null) {
            locationPermissionBackground = false;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getClaimedUserId() {
        return claimedUserId;
    }

    public void setClaimedUserId(Long claimedUserId) {
        this.claimedUserId = claimedUserId;
    }

    public Boolean getLocationPermissionForeground() {
        return locationPermissionForeground;
    }

    public void setLocationPermissionForeground(Boolean locationPermissionForeground) {
        this.locationPermissionForeground = locationPermissionForeground;
    }

    public Boolean getLocationPermissionBackground() {
        return locationPermissionBackground;
    }

    public void setLocationPermissionBackground(Boolean locationPermissionBackground) {
        this.locationPermissionBackground = locationPermissionBackground;
    }

    public LocalDateTime getPermissionUpdatedAt() {
        return permissionUpdatedAt;
    }

    public void setPermissionUpdatedAt(LocalDateTime permissionUpdatedAt) {
        this.permissionUpdatedAt = permissionUpdatedAt;
    }

    public Long getCreatedByChildId() {
        return createdByChildId;
    }

    public void setCreatedByChildId(Long createdByChildId) {
        this.createdByChildId = createdByChildId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
