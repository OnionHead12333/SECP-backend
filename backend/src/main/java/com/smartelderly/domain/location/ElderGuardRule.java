package com.smartelderly.domain.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "elder_guard_rules")
public class ElderGuardRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "elder_profile_id", nullable = false)
    private Long elderProfileId;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "active_start_time", nullable = false)
    private LocalTime activeStartTime;

    @Column(name = "active_end_time", nullable = false)
    private LocalTime activeEndTime;

    @Column(name = "home_inactivity_minutes", nullable = false)
    private Integer homeInactivityMinutes;

    @Column(name = "outside_inactivity_minutes", nullable = false)
    private Integer outsideInactivityMinutes;

    @Column(name = "alert_min_interval_minutes", nullable = false)
    private Integer alertMinIntervalMinutes;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getters and Setters
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalTime getActiveStartTime() {
        return activeStartTime;
    }

    public void setActiveStartTime(LocalTime activeStartTime) {
        this.activeStartTime = activeStartTime;
    }

    public LocalTime getActiveEndTime() {
        return activeEndTime;
    }

    public void setActiveEndTime(LocalTime activeEndTime) {
        this.activeEndTime = activeEndTime;
    }

    public Integer getHomeInactivityMinutes() {
        return homeInactivityMinutes;
    }

    public void setHomeInactivityMinutes(Integer homeInactivityMinutes) {
        this.homeInactivityMinutes = homeInactivityMinutes;
    }

    public Integer getOutsideInactivityMinutes() {
        return outsideInactivityMinutes;
    }

    public void setOutsideInactivityMinutes(Integer outsideInactivityMinutes) {
        this.outsideInactivityMinutes = outsideInactivityMinutes;
    }

    public Integer getAlertMinIntervalMinutes() {
        return alertMinIntervalMinutes;
    }

    public void setAlertMinIntervalMinutes(Integer alertMinIntervalMinutes) {
        this.alertMinIntervalMinutes = alertMinIntervalMinutes;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
