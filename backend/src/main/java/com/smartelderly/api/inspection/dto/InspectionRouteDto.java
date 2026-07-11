package com.smartelderly.api.inspection.dto;

import java.util.List;

public record InspectionRouteDto(
        String id,
        String name,
        List<String> placeIds) {
}
