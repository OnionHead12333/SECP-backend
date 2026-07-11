package com.smartelderly.api.inspection.dto;

public record FallEventRequest(
        Boolean fallAlert,
        String riskLevel,
        Long elderId,
        String elderName,
        String identitySource,
        Double identityConfidence,
        String locationName,
        Integer x,
        Integer y,
        String imageUrl,
        String time) {
}
