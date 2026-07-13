package com.smartelderly.api.control;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.entertainment.RobotCommandLog;
import com.smartelderly.api.entertainment.RobotCommandLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RobotControlControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CapturingGatewayClient gatewayClient;
    private RobotCommandLogRepository commandLogRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        gatewayClient = new CapturingGatewayClient();
        commandLogRepository = org.mockito.Mockito.mock(RobotCommandLogRepository.class);
        when(commandLogRepository.save(any(RobotCommandLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RobotControlService service = new RobotControlService(gatewayClient, commandLogRepository, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(new RobotControlController(service)).build();
    }

    @Test
    void command_shouldForwardControlCommandToGatewayAndWriteSuccessLog() throws Exception {
        gatewayClient.commandResponse = RobotGatewayCommandResponse.success("accepted", null);

        mockMvc.perform(post("/api/robot/control/command").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("{\"cmd\":\"forward\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("accepted"))
                .andExpect(jsonPath("$.data.cmd").value("forward"))
                .andExpect(jsonPath("$.data.controlBlockReason").value(nullValue()));

        org.junit.jupiter.api.Assertions.assertEquals("control", gatewayClient.lastCommand.type());
        org.junit.jupiter.api.Assertions.assertEquals("forward", gatewayClient.lastCommand.cmd());

        ArgumentCaptor<RobotCommandLog> captor = ArgumentCaptor.forClass(RobotCommandLog.class);
        verify(commandLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        RobotCommandLog finalLog = captor.getAllValues().get(1);
        org.junit.jupiter.api.Assertions.assertEquals("control", finalLog.getCommandType());
        org.junit.jupiter.api.Assertions.assertEquals("forward", finalLog.getCommand());
        org.junit.jupiter.api.Assertions.assertEquals("success", finalLog.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("accepted", finalLog.getResponseMessage());
    }

    @Test
    void command_shouldRejectUnsupportedCommandWithoutCallingGateway() throws Exception {
        mockMvc.perform(post("/api/robot/control/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cmd\":\"spin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("不支持的控制命令"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        org.junit.jupiter.api.Assertions.assertNull(gatewayClient.lastCommand);
        verify(commandLogRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void command_shouldReturnGatewayUnavailableWhenClientCannotConnect() throws Exception {
        gatewayClient.commandFailure = new RobotGatewayUnavailableException("connect failed");

        mockMvc.perform(post("/api/robot/control/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cmd\":\"stop\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("网关不可达"))
                .andExpect(jsonPath("$.data.cmd").value("stop"));

        ArgumentCaptor<RobotCommandLog> captor = ArgumentCaptor.forClass(RobotCommandLog.class);
        verify(commandLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("failed", captor.getAllValues().get(1).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("网关不可达", captor.getAllValues().get(1).getResponseMessage());
    }

    @Test
    void command_shouldKeepControlBlockReasonWhenGatewayRejectsCommand() throws Exception {
        gatewayClient.commandResponse = RobotGatewayCommandResponse.failure("控制失败", "emergency_stop_active");

        mockMvc.perform(post("/api/robot/control/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cmd\":\"forward\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("控制失败"))
                .andExpect(jsonPath("$.data.cmd").value("forward"))
                .andExpect(jsonPath("$.data.controlBlockReason").value("emergency_stop_active"));

        ArgumentCaptor<RobotCommandLog> captor = ArgumentCaptor.forClass(RobotCommandLog.class);
        verify(commandLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("failed", captor.getAllValues().get(1).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("控制失败", captor.getAllValues().get(1).getResponseMessage());
    }

    @Test
    void state_shouldFetchGatewayStateAndReturnCamelCaseFields() throws Exception {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("current_cmd", "forward");
        state.put("fall_alert", true);
        state.put("risk_level", "high");
        state.put("obstacle_status", "clear");
        state.put("navigation_status", "idle");
        state.put("control_connected", true);
        state.put("emergency_stop", false);
        state.put("control_block_reason", "none");
        state.put("last_command_time", "2026-07-12T10:00:00");
        state.put("timeout_sec", 3);
        gatewayClient.state = state;

        mockMvc.perform(get("/api/robot/control/state").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentCmd").value("forward"))
                .andExpect(jsonPath("$.data.fallAlert").value(true))
                .andExpect(jsonPath("$.data.riskLevel").value("high"))
                .andExpect(jsonPath("$.data.obstacleStatus").value("clear"))
                .andExpect(jsonPath("$.data.navigationStatus").value("idle"))
                .andExpect(jsonPath("$.data.controlConnected").value(true))
                .andExpect(jsonPath("$.data.emergencyStop").value(false))
                .andExpect(jsonPath("$.data.controlBlockReason").value("none"))
                .andExpect(jsonPath("$.data.lastCommandTime").value("2026-07-12T10:00:00"))
                .andExpect(jsonPath("$.data.timeoutSec").value(3));
    }

    @Test
    void state_shouldReturnGatewayUnavailableWhenStateFetchFails() throws Exception {
        gatewayClient.stateFailure = new RobotGatewayUnavailableException("connect failed");

        mockMvc.perform(get("/api/robot/control/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("网关不可达"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void httpGatewayClient_shouldBeCreatedBySpringContainer() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RobotGatewayProperties.class);
            context.registerBean(ObjectMapper.class);
            context.register(HttpRobotGatewayClient.class);

            context.refresh();

            org.junit.jupiter.api.Assertions.assertNotNull(context.getBean(RobotGatewayClient.class));
        }
    }

    private static class CapturingGatewayClient implements RobotGatewayClient {
        private RobotGatewayCommandRequest lastCommand;
        private RobotGatewayCommandResponse commandResponse = RobotGatewayCommandResponse.success("ok", null);
        private RobotGatewayUnavailableException commandFailure;
        private Map<String, Object> state = Map.of();
        private RobotGatewayUnavailableException stateFailure;

        @Override
        public RobotGatewayCommandResponse sendCommand(RobotGatewayCommandRequest request) {
            if (commandFailure != null) {
                throw commandFailure;
            }
            lastCommand = request;
            return commandResponse;
        }

        @Override
        public Map<String, Object> fetchState() {
            if (stateFailure != null) {
                throw stateFailure;
            }
            return state;
        }
    }
}
