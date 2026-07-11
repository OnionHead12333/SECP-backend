package com.smartelderly.api.inspection.dto;

public record NavigationStatusDto(
        String navigationStatus,
        int currentX,
        int currentY,
        int targetX,
        int targetY,
        String targetName,
        String message) {
}
