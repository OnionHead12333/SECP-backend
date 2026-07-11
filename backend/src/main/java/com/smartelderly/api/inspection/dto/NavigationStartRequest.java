package com.smartelderly.api.inspection.dto;

public record NavigationStartRequest(
        String targetName,
        Integer targetX,
        Integer targetY) {
}
