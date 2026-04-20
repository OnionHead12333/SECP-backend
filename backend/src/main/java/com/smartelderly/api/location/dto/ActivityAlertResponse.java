package com.smartelderly.api.location.dto;

public class ActivityAlertResponse {

    private Long alertId; //告警ID
    private String alertType;//告警类型
    private String triggerMode;//触发方式(按钮、规则等)
    private String title;//告警标题
    private String content;//告警内容
    private String triggeredAt;//告警触发时间，ISO 8601格式

    public ActivityAlertResponse() {
    }

    public ActivityAlertResponse(Long alertId, String alertType, String triggerMode,
            String title, String content, String triggeredAt) {
        this.alertId = alertId;
        this.alertType = alertType;
        this.triggerMode = triggerMode;
        this.title = title;
        this.content = content;
        this.triggeredAt = triggeredAt;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(String triggerMode) {
        this.triggerMode = triggerMode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(String triggeredAt) {
        this.triggeredAt = triggeredAt;
    }
}
