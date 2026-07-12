package com.smartelderly.api.control;

import java.util.Map;

public interface RobotGatewayClient {

    RobotGatewayCommandResponse sendCommand(RobotGatewayCommandRequest request);

    Map<String, Object> fetchState();
}
