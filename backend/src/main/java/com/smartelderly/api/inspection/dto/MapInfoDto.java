package com.smartelderly.api.inspection.dto;

public record MapInfoDto(
        long mapId,
        String mapName,
        String mapImage,
        int width,
        int height,
        double resolution,
        double originX,
        double originY,
        double originYaw,
        int imageHeight) {
}
