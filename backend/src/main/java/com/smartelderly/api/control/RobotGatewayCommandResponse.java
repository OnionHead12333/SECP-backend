package com.smartelderly.api.control;

public record RobotGatewayCommandResponse(
        boolean success,
        String message,
        String controlBlockReason) {

    public static RobotGatewayCommandResponse success(String message, String controlBlockReason) {
        return new RobotGatewayCommandResponse(true, message, controlBlockReason);
    }

    public static RobotGatewayCommandResponse failure(String message, String controlBlockReason) {
        return new RobotGatewayCommandResponse(false, message, controlBlockReason);
    }
}
