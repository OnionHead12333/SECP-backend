package com.smartelderly.api.voice;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VoiceCommandResponse(
        String command,
        Long taskId,
        String status,
        String feedback) {
}
