package com.smartelderly.api.device.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceHeartbeatRequest(
        @NotBlank(message = "deviceId required") String deviceId,
        @NotBlank(message = "secret required") String secret,
        String status,
        Integer signalStrength,
        String reportedAt) {
}
