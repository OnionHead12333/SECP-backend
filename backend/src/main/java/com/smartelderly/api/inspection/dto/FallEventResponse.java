package com.smartelderly.api.inspection.dto;

public record FallEventResponse(
        long fallEventId,
        long markerId,
        long alertId,
        boolean notifiedChild) {
}
