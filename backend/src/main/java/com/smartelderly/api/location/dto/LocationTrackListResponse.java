package com.smartelderly.api.location.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 位置轨迹列表响应
 */
public class LocationTrackListResponse {

    private List<TrackPoint> points;
    private String summary;

    public static class TrackPoint {
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String label;
        private String recordedAt;
        private String locationType;

        public TrackPoint(BigDecimal latitude, BigDecimal longitude, String label, String recordedAt, String locationType) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.label = label;
            this.recordedAt = recordedAt;
            this.locationType = locationType;
        }

        // Getters and Setters
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

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getRecordedAt() {
            return recordedAt;
        }

        public void setRecordedAt(String recordedAt) {
            this.recordedAt = recordedAt;
        }

        public String getLocationType() {
            return locationType;
        }

        public void setLocationType(String locationType) {
            this.locationType = locationType;
        }
    }

    public LocationTrackListResponse(List<TrackPoint> points, String summary) {
        this.points = points;
        this.summary = summary;
    }

    public List<TrackPoint> getPoints() {
        return points;
    }

    public void setPoints(List<TrackPoint> points) {
        this.points = points;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
