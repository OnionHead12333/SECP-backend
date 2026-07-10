package com.smartelderly.api.location.dto;

//返回给前端的监护规则信息DTO
public class GuardRuleResponse {

    private Boolean enabled; //监护规则是否启用
    private String activeStartTime;//监护规则的生效开始时间，格式为"HH:mm"，例如"08:00"
    private String activeEndTime;//监护规则的生效结束时间，格式为"HH:mm"，例如"08:00"
    private Integer homeInactivityMinutes;//在家的不活动分钟数阈值
    private Integer outsideInactivityMinutes;//在室外的不活动分钟数阈值
    private Integer alertMinIntervalMinutes;//告警最小间隔时间，单位为分钟

    public GuardRuleResponse() {
    }

    public GuardRuleResponse(Boolean enabled, String activeStartTime, String activeEndTime,
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
