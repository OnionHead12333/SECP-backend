package com.smartelderly.api.location.dto;

import java.math.BigDecimal;

public class HomeGeofenceResponse {

    private String name;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private Integer radius;
    private Boolean enabled;

    public HomeGeofenceResponse() {
    }

    public HomeGeofenceResponse(String name, BigDecimal centerLatitude, BigDecimal centerLongitude,
            Integer radius, Boolean enabled) {
        this.name = name;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.radius = radius;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(BigDecimal centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public BigDecimal getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(BigDecimal centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public Integer getRadius() {
        return radius;
    }

    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
