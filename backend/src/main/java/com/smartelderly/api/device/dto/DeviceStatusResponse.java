package com.smartelderly.api.device.dto;

import java.time.Instant;

public record DeviceStatusResponse(
        String deviceId,
        Long familyId,
        String status,
        String installArea,
        Instant lastHeartbeatAt,
        Instant lastSeenAt,
        Integer signalStrength) {
}
