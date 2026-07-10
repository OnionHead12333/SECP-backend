package com.smartelderly.api.device.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceSosRequest(
        @NotBlank(message = "deviceId required") String deviceId,
        @NotBlank(message = "secret required") String secret,
        @NotBlank(message = "eventType required") String eventType,
        String area,
        String message,
        String triggeredAt) {
}
