package com.smartelderly.api.entertainment;

public record MusicResponse(
        Long id,
        String musicName,
        String musicUrl,
        String artist,
        Integer durationSeconds,
        String suitableScene) {
}
