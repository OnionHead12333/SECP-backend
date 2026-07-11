package com.smartelderly.api.inspection.dto;

public record ObstacleStatusDto(
        String obstacleStatus,
        Integer x,
        Integer y,
        String message) {
}
