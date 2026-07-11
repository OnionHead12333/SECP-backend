package com.smartelderly.api.entertainment;

public record PendingEntertainmentTaskResponse(
        Long taskId,
        Long robotId,
        Long userId,
        String taskType,
        Long musicId,
        String musicName,
        String musicUrl,
        String danceMode,
        String status,
        String responseMessage,
        String createdAt) {
}
