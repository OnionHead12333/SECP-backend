package com.smartelderly.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "elder_profiles")
public class ElderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 30)
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
}
