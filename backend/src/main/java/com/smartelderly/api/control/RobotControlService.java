package com.smartelderly.api.control;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.entertainment.RobotCommandLog;
import com.smartelderly.api.entertainment.RobotCommandLogRepository;
import com.smartelderly.api.inspection.InspectionApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RobotControlService {

    private static final Set<String> SUPPORTED_COMMANDS = Set.of(
            "forward",
            "backward",
            "left",
            "right",
            "stop",
            "emergency_stop",
            "reset_emergency");

    private final RobotGatewayClient gatewayClient;
    private final RobotCommandLogRepository commandLogRepository;
    private final ObjectMapper objectMapper;

    public RobotControlService(
            RobotGatewayClient gatewayClient,
            RobotCommandLogRepository commandLogRepository,
            ObjectMapper objectMapper) {
        this.gatewayClient = gatewayClient;
        this.commandLogRepository = commandLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InspectionApiResponse<RobotControlCommandResult> sendCommand(
            RobotControlCommandRequest request,
            long userId) {
        String cmd = normalizeCommand(request == null ? null : request.cmd());
        if (!SUPPORTED_COMMANDS.contains(cmd)) {
            return InspectionApiResponse.fail("不支持的控制命令");
        }

        RobotGatewayCommandRequest gatewayRequest = new RobotGatewayCommandRequest("control", cmd);
        RobotCommandLog commandLog = createSentLog(cmd, gatewayRequest, userId);
        commandLogRepository.save(commandLog);

        try {
            RobotGatewayCommandResponse gatewayResponse = gatewayClient.sendCommand(gatewayRequest);
            String message = firstPresent(gatewayResponse.message(), gatewayResponse.success() ? "ok" : "控制失败");
            commandLog.setStatus(gatewayResponse.success() ? "success" : "failed");
            commandLog.setResponseMessage(message);
            commandLogRepository.save(commandLog);
            RobotControlCommandResult result = new RobotControlCommandResult(cmd, gatewayResponse.controlBlockReason());
            return new InspectionApiResponse<>(gatewayResponse.success(), message, result);
        } catch (RobotGatewayUnavailableException e) {
            commandLog.setStatus("failed");
            commandLog.setResponseMessage("网关不可达");
            commandLogRepository.save(commandLog);
            return new InspectionApiResponse<>(false, "网关不可达", new RobotControlCommandResult(cmd, null));
        }
    }

    public InspectionApiResponse<RobotControlStateResponse> getState() {
        try {
            return InspectionApiResponse.ok(toStateResponse(gatewayClient.fetchState()));
        } catch (RobotGatewayUnavailableException e) {
            return InspectionApiResponse.fail("网关不可达");
        }
    }

    private RobotCommandLog createSentLog(
            String cmd,
            RobotGatewayCommandRequest gatewayRequest,
            long userId) {
        RobotCommandLog commandLog = new RobotCommandLog();
        commandLog.setUserId(userId);
        commandLog.setCommandType("control");
        commandLog.setCommand(cmd);
        commandLog.setRequestJson(toJson(gatewayRequest));
        commandLog.setStatus("sent");
        commandLog.setResponseMessage("sent");
        return commandLog;
    }

    private RobotControlStateResponse toStateResponse(Map<String, Object> state) {
        return new RobotControlStateResponse(
                stringValue(state, "current_cmd", "currentCmd"),
                booleanValue(state, "fall_alert", "fallAlert"),
                stringValue(state, "risk_level", "riskLevel"),
                stringValue(state, "obstacle_status", "obstacleStatus"),
                stringValue(state, "navigation_status", "navigationStatus"),
                booleanValue(state, "control_connected", "controlConnected"),
                booleanValue(state, "emergency_stop", "emergencyStop"),
                stringValue(state, "control_block_reason", "controlBlockReason"),
                stringValue(state, "last_command_time", "lastCommandTime"),
                integerValue(state, "timeout_sec", "timeoutSec"));
    }

    private String toJson(RobotGatewayCommandRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid robot control command", e);
        }
    }

    private static String normalizeCommand(String cmd) {
        return cmd == null ? "" : cmd.trim().toLowerCase();
    }

    private static String stringValue(Map<String, Object> source, String snakeName, String camelName) {
        Object value = firstValue(source, snakeName, camelName);
        return value == null ? null : String.valueOf(value);
    }

    private static Boolean booleanValue(Map<String, Object> source, String snakeName, String camelName) {
        Object value = firstValue(source, snakeName, camelName);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value == null ? null : Boolean.valueOf(String.valueOf(value));
    }

    private static Integer integerValue(Map<String, Object> source, String snakeName, String camelName) {
        Object value = firstValue(source, snakeName, camelName);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Object firstValue(Map<String, Object> source, String snakeName, String camelName) {
        if (source == null) {
            return null;
        }
        return source.containsKey(snakeName) ? source.get(snakeName) : source.get(camelName);
    }

    private static String firstPresent(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
