package com.smartelderly.api.device.dto;

public record DeviceSosResponse(
        boolean success,
        Long alertId,
        String message) {
}
