package com.smartelderly.api.location.dto;

import java.time.LocalDateTime;

import com.smartelderly.domain.location.ElderLocationGuardSetting;

public class LocationGuardResponse {

    private Long elderProfileId;
    private Boolean enabled;
    private String mode;
    private Integer intervalSeconds;
    private Integer outsideIntervalSeconds;
    private Boolean backgroundRequired;
    private Boolean foregroundGranted;
    private Boolean backgroundGranted;
    private Boolean batteryOptimizationIgnored;
    private LocalDateTime lastStartedAt;
    private LocalDateTime lastStoppedAt;
    private LocalDateTime lastUploadAt;
    private String lastError;
    private LocalDateTime updatedAt;

    public static LocationGuardResponse from(ElderLocationGuardSetting setting) {
        LocationGuardResponse response = new LocationGuardResponse();
        response.setElderProfileId(setting.getElderProfileId());
        response.setEnabled(setting.getEnabled());
        response.setMode(setting.getMode());
        response.setIntervalSeconds(setting.getIntervalSeconds());
        response.setOutsideIntervalSeconds(setting.getOutsideIntervalSeconds());
        response.setBackgroundRequired(setting.getBackgroundRequired());
        response.setForegroundGranted(setting.getForegroundGranted());
        response.setBackgroundGranted(setting.getBackgroundGranted());
        response.setBatteryOptimizationIgnored(setting.getBatteryOptimizationIgnored());
        response.setLastStartedAt(setting.getLastStartedAt());
        response.setLastStoppedAt(setting.getLastStoppedAt());
        response.setLastUploadAt(setting.getLastUploadAt());
        response.setLastError(setting.getLastError());
        response.setUpdatedAt(setting.getUpdatedAt());
        return response;
    }

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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
