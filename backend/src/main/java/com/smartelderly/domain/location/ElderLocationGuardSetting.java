package com.smartelderly.domain.location;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "elder_location_guard_settings")
public class ElderLocationGuardSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "elder_profile_id", nullable = false)
    private Long elderProfileId;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false, length = 32)
    private String mode;

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds;

    @Column(name = "outside_interval_seconds", nullable = false)
    private Integer outsideIntervalSeconds;

    @Column(name = "background_required", nullable = false)
    private Boolean backgroundRequired;

    @Column(name = "foreground_granted", nullable = false)
    private Boolean foregroundGranted;

    @Column(name = "background_granted", nullable = false)
    private Boolean backgroundGranted;

    @Column(name = "battery_optimization_ignored", nullable = false)
    private Boolean batteryOptimizationIgnored;

    @Column(name = "last_started_at")
    private LocalDateTime lastStartedAt;

    @Column(name = "last_stopped_at")
    private LocalDateTime lastStoppedAt;

    @Column(name = "last_upload_at")
    private LocalDateTime lastUploadAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void applyDefaults() {
        LocalDateTime now = LocalDateTime.now();
        if (enabled == null) enabled = false;
        if (mode == null || mode.isBlank()) mode = "off";
        if (intervalSeconds == null) intervalSeconds = 600;
        if (outsideIntervalSeconds == null) outsideIntervalSeconds = 300;
        if (backgroundRequired == null) backgroundRequired = true;
        if (foregroundGranted == null) foregroundGranted = false;
        if (backgroundGranted == null) backgroundGranted = false;
        if (batteryOptimizationIgnored == null) batteryOptimizationIgnored = false;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getElderProfileId() { return elderProfileId; }
    public void setElderProfileId(Long elderProfileId) { this.elderProfileId = elderProfileId; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public Integer getOutsideIntervalSeconds() { return outsideIntervalSeconds; }
    public void setOutsideIntervalSeconds(Integer outsideIntervalSeconds) { this.outsideIntervalSeconds = outsideIntervalSeconds; }
    public Boolean getBackgroundRequired() { return backgroundRequired; }
    public void setBackgroundRequired(Boolean backgroundRequired) { this.backgroundRequired = backgroundRequired; }
    public Boolean getForegroundGranted() { return foregroundGranted; }
    public void setForegroundGranted(Boolean foregroundGranted) { this.foregroundGranted = foregroundGranted; }
    public Boolean getBackgroundGranted() { return backgroundGranted; }
    public void setBackgroundGranted(Boolean backgroundGranted) { this.backgroundGranted = backgroundGranted; }
    public Boolean getBatteryOptimizationIgnored() { return batteryOptimizationIgnored; }
    public void setBatteryOptimizationIgnored(Boolean batteryOptimizationIgnored) { this.batteryOptimizationIgnored = batteryOptimizationIgnored; }
    public LocalDateTime getLastStartedAt() { return lastStartedAt; }
    public void setLastStartedAt(LocalDateTime lastStartedAt) { this.lastStartedAt = lastStartedAt; }
    public LocalDateTime getLastStoppedAt() { return lastStoppedAt; }
    public void setLastStoppedAt(LocalDateTime lastStoppedAt) { this.lastStoppedAt = lastStoppedAt; }
    public LocalDateTime getLastUploadAt() { return lastUploadAt; }
    public void setLastUploadAt(LocalDateTime lastUploadAt) { this.lastUploadAt = lastUploadAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
