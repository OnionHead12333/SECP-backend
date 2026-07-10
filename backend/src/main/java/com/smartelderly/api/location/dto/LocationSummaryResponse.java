package com.smartelderly.api.location.dto;

import java.math.BigDecimal;

public class LocationSummaryResponse {

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private Boolean isHome;
    private String presenceSource;
    private String updatedAt;

    public LocationSummaryResponse() {
    }

    public LocationSummaryResponse(BigDecimal latitude, BigDecimal longitude, String address,
            Boolean isHome, String presenceSource, String updatedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.isHome = isHome;
        this.presenceSource = presenceSource;
        this.updatedAt = updatedAt;
    }

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getIsHome() {
        return isHome;
    }

    public void setIsHome(Boolean isHome) {
        this.isHome = isHome;
    }

    public String getPresenceSource() {
        return presenceSource;
    }

    public void setPresenceSource(String presenceSource) {
        this.presenceSource = presenceSource;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
