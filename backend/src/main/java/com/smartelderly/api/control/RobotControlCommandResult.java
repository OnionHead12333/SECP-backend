package com.smartelderly.api.control;

public record RobotControlCommandResult(
        String cmd,
        String controlBlockReason) {
}
