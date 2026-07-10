package com.smartelderly.api.location.dto;

public class StartLocationGuardRequest {

    private String mode;
    private Integer intervalSeconds;
    private Integer outsideIntervalSeconds;
    private Boolean backgroundRequired;
    private Boolean foregroundGranted;
    private Boolean backgroundGranted;
    private Boolean batteryOptimizationIgnored;

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
}
