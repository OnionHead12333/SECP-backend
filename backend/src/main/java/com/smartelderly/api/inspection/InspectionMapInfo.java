package com.smartelderly.api.inspection;

public record InspectionMapInfo(
        long mapId,
        String mapName,
        String mapImage,
        int width,
        int height) {
}
