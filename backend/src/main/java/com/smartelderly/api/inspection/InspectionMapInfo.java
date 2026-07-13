package com.smartelderly.api.inspection;

public record InspectionMapInfo(
        String mapId,
        String mapName,
        String mapImage,
        int width,
        int height,
        double resolution,
        double originX,
        double originY,
        int imageHeight) {
}
