package com.smartelderly.api.location.dto;

import java.math.BigDecimal;

//请求体中表示位置轨迹信息的DTO
public class LocationTrackDTO {

    private BigDecimal latitude;   //纬度
    private BigDecimal longitude;  //经度
    private String locationType;  // 位置类型: indoor/outdoor
    private String source;        // 定位来源: gaode/gps/wifi/beacon/sensor
    private String recordedAt;    // 记录时间: ISO 8601格式

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }
}
