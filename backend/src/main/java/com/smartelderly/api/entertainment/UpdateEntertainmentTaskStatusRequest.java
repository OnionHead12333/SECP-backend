package com.smartelderly.api.entertainment;

public record UpdateEntertainmentTaskStatusRequest(
        String status,
        String message) {
}
