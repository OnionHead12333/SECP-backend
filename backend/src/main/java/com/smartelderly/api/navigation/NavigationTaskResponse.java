package com.smartelderly.api.navigation;

public record NavigationTaskResponse(
        Long taskId,
        String status,
        Double targetX,
        Double targetY,
        String targetName) {
}
