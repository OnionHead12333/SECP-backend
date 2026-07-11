package com.smartelderly.api.inspection.dto;

public record InspectionPlaceDto(
        String id,
        String name,
        int x,
        int y,
        String locationName) {
}
