package com.smartelderly.api.inspection;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class InspectionMarker {

    private Long id;
    private Long mapId;
    private Long elderId;
    private String type;
    private String title;
    private String message;
    private String description;
    private Double x;
    private Double y;
    private String level;
    private String status;
    private String source;
    private String locationName;
    private String elderName;
    private String identitySource;
    private Double identityConfidence;
    private Boolean notifiedChild;
    private String imageUrl;
    private String time;
    private String navigationStatus;
    private String obstacleStatus;
    private String payloadJson;
    private String handler;
    private String remark;
    private String handleTime;

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

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getElderProfileId() {
        return elderId;
    }

    public void setElderProfileId(Long elderProfileId) {
        this.elderId = elderProfileId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message == null ? description : message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(String handleTime) {
        this.handleTime = handleTime;
    }
}
