package com.smartelderly.api.entertainment;

public record EntertainmentStatusResponse(
        Long currentTaskId,
        String taskType,
        String musicName,
        String danceMode,
        String status,
        String responseMessage) {
}
