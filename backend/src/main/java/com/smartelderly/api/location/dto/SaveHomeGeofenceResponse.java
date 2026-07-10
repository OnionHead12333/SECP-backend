package com.smartelderly.api.location.dto;

public class SaveHomeGeofenceResponse {

    private Long elderId;

    public SaveHomeGeofenceResponse() {
    }

    public SaveHomeGeofenceResponse(Long elderId) {
        this.elderId = elderId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }
}
