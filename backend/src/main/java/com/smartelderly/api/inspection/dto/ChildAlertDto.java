package com.smartelderly.api.inspection.dto;

public record ChildAlertDto(
        long id,
        String type,
        Long elderId,
        String elderName,
        String message,
        String locationName,
        String imageUrl,
        String time,
        String status,
        boolean notified) {
}
