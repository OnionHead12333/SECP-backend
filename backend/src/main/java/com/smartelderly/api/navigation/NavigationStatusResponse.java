package com.smartelderly.api.navigation;

public record NavigationStatusResponse(
        String navigationStatus,
        Double currentX,
        Double currentY,
        Double targetX,
        Double targetY,
        String targetName,
        String message) {
}
