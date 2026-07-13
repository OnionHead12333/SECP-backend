package com.smartelderly.api.control;

public record RobotControlStateResponse(
        String currentCmd,
        Boolean fallAlert,
        String riskLevel,
        String obstacleStatus,
        String navigationStatus,
        Boolean controlConnected,
        Boolean emergencyStop,
        String controlBlockReason,
        String lastCommandTime,
        Integer timeoutSec) {
}
