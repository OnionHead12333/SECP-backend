package com.smartelderly.api.inspection.dto;

public record InspectionMarkerDto(
        long id,
        String type,
        int x,
        int y,
        String level,
        String title,
        String message,
        String imageUrl,
        String time,
        String status,
        String locationName,
        Long elderId,
        String elderName,
        String identitySource,
        Double identityConfidence,
        Boolean notifiedChild) {

    public InspectionMarkerDto withId(long nextId) {
        return new InspectionMarkerDto(
                nextId,
                type,
                x,
                y,
                level,
                title,
                message,
                imageUrl,
                time,
                status,
                locationName,
                elderId,
                elderName,
                identitySource,
                identityConfidence,
                notifiedChild);
    }

    public InspectionMarkerDto handled() {
        return new InspectionMarkerDto(
                id,
                type,
                x,
                y,
                level,
                title,
                message,
                imageUrl,
                time,
                "handled",
                locationName,
                elderId,
                elderName,
                identitySource,
                identityConfidence,
                notifiedChild);
    }
}
