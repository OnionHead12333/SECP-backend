package com.smartelderly.api.entertainment;

public record EntertainmentCommandRequest(
        Long robotId,
        Long userId,
        Long musicId,
        String musicName,
        String musicUrl,
        String danceMode) {
}
