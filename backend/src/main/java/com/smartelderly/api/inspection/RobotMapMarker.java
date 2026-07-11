package com.smartelderly.api.inspection;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "robot_map_marker")
public class RobotMapMarker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "map_id", nullable = false)
    private Long mapId;

    @Column(name = "robot_id")
    private Long robotId;

    @Column(name = "elder_profile_id")
    private Long elderProfileId;

    @Column(name = "marker_type", nullable = false, length = 32)
    private String markerType;

    @Column(length = 32)
    private String level;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "location_name", length = 100)
    private String locationName;

    @Column(name = "location_x", nullable = false)
    private Double locationX;

    @Column(name = "location_y", nullable = false)
    private Double locationY;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "elder_name", length = 50)
    private String elderName;

    @Column(name = "identity_source", length = 32)
    private String identitySource;

    @Column(name = "identity_confidence")
    private Double identityConfidence;

    @Column(name = "notified_child")
    private Boolean notifiedChild;

    @Column(name = "navigation_status", length = 32)
    private String navigationStatus;

    @Column(name = "obstacle_status", length = 32)
    private String obstacleStatus;

    @Column(length = 50)
    private String source;

    @Column(length = 32)
    private String status;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_by_name", length = 50)
    private String handledByName;

    @Column(name = "handle_remark", length = 500)
    private String handleRemark;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (mapId == null) {
            mapId = 1L;
        }
        if (level == null) {
            level = "info";
        }
        if (source == null) {
            source = "mock";
        }
        if (status == null) {
            status = "unhandled";
        }
        if (notifiedChild == null) {
            notifiedChild = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMapId() {
        return mapId;
    }

    public void setMapId(Long mapId) {
        this.mapId = mapId;
    }

    public Long getRobotId() {
        return robotId;
    }

    public void setRobotId(Long robotId) {
        this.robotId = robotId;
    }

    public Long getElderProfileId() {
        return elderProfileId;
    }

    public void setElderProfileId(Long elderProfileId) {
        this.elderProfileId = elderProfileId;
    }

    public String getMarkerType() {
        return markerType;
    }

    public void setMarkerType(String markerType) {
        this.markerType = markerType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Double getLocationX() {
        return locationX;
    }

    public void setLocationX(Double locationX) {
        this.locationX = locationX;
    }

    public Double getLocationY() {
        return locationY;
    }

    public void setLocationY(Double locationY) {
        this.locationY = locationY;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getElderName() {
        return elderName;
    }

    public void setElderName(String elderName) {
        this.elderName = elderName;
    }

    public String getIdentitySource() {
        return identitySource;
    }

    public void setIdentitySource(String identitySource) {
        this.identitySource = identitySource;
    }

    public Double getIdentityConfidence() {
        return identityConfidence;
    }

    public void setIdentityConfidence(Double identityConfidence) {
        this.identityConfidence = identityConfidence;
    }

    public Boolean getNotifiedChild() {
        return notifiedChild;
    }

    public void setNotifiedChild(Boolean notifiedChild) {
        this.notifiedChild = notifiedChild;
    }

    public String getNavigationStatus() {
        return navigationStatus;
    }

    public void setNavigationStatus(String navigationStatus) {
        this.navigationStatus = navigationStatus;
    }

    public String getObstacleStatus() {
        return obstacleStatus;
    }

    public void setObstacleStatus(String obstacleStatus) {
        this.obstacleStatus = obstacleStatus;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(Long handledBy) {
        this.handledBy = handledBy;
    }

    public String getHandledByName() {
        return handledByName;
    }

    public void setHandledByName(String handledByName) {
        this.handledByName = handledByName;
    }

    public String getHandleRemark() {
        return handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
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
