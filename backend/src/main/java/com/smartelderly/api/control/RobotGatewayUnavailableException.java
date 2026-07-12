package com.smartelderly.api.control;

public class RobotGatewayUnavailableException extends RuntimeException {

    public RobotGatewayUnavailableException(String message) {
        super(message);
    }

    public RobotGatewayUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
