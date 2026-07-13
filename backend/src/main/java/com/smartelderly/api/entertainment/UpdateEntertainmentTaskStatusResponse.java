package com.smartelderly.api.entertainment;

public record UpdateEntertainmentTaskStatusResponse(
        Long taskId,
        String status,
        String responseMessage) {
}
