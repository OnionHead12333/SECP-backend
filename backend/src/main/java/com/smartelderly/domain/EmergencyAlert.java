package com.smartelderly.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "emergency_alerts")
public class EmergencyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "elder_profile_id", nullable = false)
    private Long elderProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 32)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_mode", length = 32)
    private TriggerMode triggerMode = TriggerMode.button;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AlertStatus status;

    @Column(name = "trigger_time")
    private LocalDateTime triggerTime;

    @Column(name = "revoke_deadline")
    private LocalDateTime revokeDeadline;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_mode", length = 32)
    private CancelMode cancelMode;

    @Column(name = "handled_time")
    private LocalDateTime handledTime;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

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

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public TriggerMode getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(TriggerMode triggerMode) {
        this.triggerMode = triggerMode;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public LocalDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(LocalDateTime triggerTime) {
        this.triggerTime = triggerTime;
    }

    public LocalDateTime getRevokeDeadline() {
        return revokeDeadline;
    }

    public void setRevokeDeadline(LocalDateTime revokeDeadline) {
        this.revokeDeadline = revokeDeadline;
    }

    public LocalDateTime getSentTime() {
        return sentTime;
    }

    public void setSentTime(LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }

    public LocalDateTime getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(LocalDateTime cancelTime) {
        this.cancelTime = cancelTime;
    }

    public CancelMode getCancelMode() {
        return cancelMode;
    }

    public void setCancelMode(CancelMode cancelMode) {
        this.cancelMode = cancelMode;
    }

    public LocalDateTime getHandledTime() {
        return handledTime;
    }

    public void setHandledTime(LocalDateTime handledTime) {
        this.handledTime = handledTime;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(Long handledBy) {
        this.handledBy = handledBy;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
