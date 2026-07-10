package com.smartelderly.api.location.dto;

import java.time.LocalDateTime;

public class LocationPermissionResponse {

    private Boolean foregroundGranted;
    private Boolean backgroundGranted;
    private LocalDateTime permissionUpdatedAt;

    public LocationPermissionResponse() {
    }

    public LocationPermissionResponse(Boolean foregroundGranted, Boolean backgroundGranted,
            LocalDateTime permissionUpdatedAt) {
        this.foregroundGranted = foregroundGranted;
        this.backgroundGranted = backgroundGranted;
        this.permissionUpdatedAt = permissionUpdatedAt;
    }

    public Boolean getForegroundGranted() {
        return foregroundGranted;
    }

    public void setForegroundGranted(Boolean foregroundGranted) {
        this.foregroundGranted = foregroundGranted;
    }

    public Boolean getBackgroundGranted() {
        return backgroundGranted;
    }

    public void setBackgroundGranted(Boolean backgroundGranted) {
        this.backgroundGranted = backgroundGranted;
    }

    public LocalDateTime getPermissionUpdatedAt() {
        return permissionUpdatedAt;
    }

    public void setPermissionUpdatedAt(LocalDateTime permissionUpdatedAt) {
        this.permissionUpdatedAt = permissionUpdatedAt;
    }
}
