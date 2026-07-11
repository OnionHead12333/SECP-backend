package com.smartelderly.api.inspection.dto;

public record NavigationStatusUpdateRequest(
        String navigationStatus,
        Integer currentX,
        Integer currentY,
        Integer targetX,
        Integer targetY,
        String targetName,
        String message) {
}
