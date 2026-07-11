package com.smartelderly.api.voice;

public record VoiceCommandRequest(
        String command,
        Long robotId,
        Long userId,
        Long musicId,
        String musicName,
        String musicUrl,
        String danceMode) {
}
