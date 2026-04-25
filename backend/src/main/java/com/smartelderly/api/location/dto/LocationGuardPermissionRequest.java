package com.smartelderly.api.location.dto;

public class LocationGuardPermissionRequest {

    private Boolean foregroundGranted;
    private Boolean backgroundGranted;
    private Boolean batteryOptimizationIgnored;

    public Boolean getForegroundGranted() { return foregroundGranted; }
    public void setForegroundGranted(Boolean foregroundGranted) { this.foregroundGranted = foregroundGranted; }
    public Boolean getBackgroundGranted() { return backgroundGranted; }
    public void setBackgroundGranted(Boolean backgroundGranted) { this.backgroundGranted = backgroundGranted; }
    public Boolean getBatteryOptimizationIgnored() { return batteryOptimizationIgnored; }
    public void setBatteryOptimizationIgnored(Boolean batteryOptimizationIgnored) { this.batteryOptimizationIgnored = batteryOptimizationIgnored; }
}
