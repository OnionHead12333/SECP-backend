package com.smartelderly.api.navigation;

public record NavigationTaskRequest(
        Long robotId,
        Long creatorId,
        Long mapId,
        String targetName,
        Double targetX,
        Double targetY) {
}
