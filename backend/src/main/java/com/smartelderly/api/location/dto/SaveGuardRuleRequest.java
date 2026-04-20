package com.smartelderly.api.location.dto;

public class SaveGuardRuleRequest {

    private Boolean enabled;
    private String activeStartTime;
    private String activeEndTime;
    private Integer homeInactivityMinutes;
    private Integer outsideInactivityMinutes;
    private Integer alertMinIntervalMinutes;

    public SaveGuardRuleRequest() {
    }

    public SaveGuardRuleRequest(Boolean enabled, String activeStartTime, String activeEndTime,
            Integer homeInactivityMinutes, Integer outsideInactivityMinutes,
            Integer alertMinIntervalMinutes) {
        this.enabled = enabled;
        this.activeStartTime = activeStartTime;
        this.activeEndTime = activeEndTime;
        this.homeInactivityMinutes = homeInactivityMinutes;
        this.outsideInactivityMinutes = outsideInactivityMinutes;
        this.alertMinIntervalMinutes = alertMinIntervalMinutes;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getActiveStartTime() {
        return activeStartTime;
    }

    public void setActiveStartTime(String activeStartTime) {
        this.activeStartTime = activeStartTime;
    }

    public String getActiveEndTime() {
        return activeEndTime;
    }

    public void setActiveEndTime(String activeEndTime) {
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
}
