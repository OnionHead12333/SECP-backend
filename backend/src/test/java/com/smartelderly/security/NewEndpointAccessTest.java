package com.smartelderly.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.smartelderly.api.GlobalExceptionHandler;
import com.smartelderly.api.HealthController;
import com.smartelderly.api.child.ChildFallAlertController;
import com.smartelderly.api.child.ChildFallAlertService;
import com.smartelderly.api.control.RobotControlCommandRequest;
import com.smartelderly.api.control.RobotControlCommandResult;
import com.smartelderly.api.control.RobotControlController;
import com.smartelderly.api.control.RobotControlService;
import com.smartelderly.api.entertainment.EntertainmentController;
import com.smartelderly.api.entertainment.EntertainmentService;
import com.smartelderly.api.inspection.InspectionApiResponse;
import com.smartelderly.api.inspection.InspectionMarkerController;
import com.smartelderly.api.inspection.InspectionMarkerService;
import com.smartelderly.api.navigation.NavigationTaskController;
import com.smartelderly.api.navigation.NavigationTaskService;
import com.smartelderly.api.voice.VoiceCommandController;
import com.smartelderly.api.voice.VoiceCommandService;
import com.smartelderly.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@WebMvcTest(
        controllers = {
                RobotControlController.class,
                ChildFallAlertController.class,
                NavigationTaskController.class,
                EntertainmentController.class,
                VoiceCommandController.class,
                InspectionMarkerController.class,
                HealthController.class
        },
        properties = {
                "spring.profiles.active=test",
                "app.jwt.secret=test-only-jwt-secret-key-at-least-32-characters"
        })
@EnableConfigurationProperties(AppProperties.class)
@Import({
        SecurityConfig.class,
        JwtService.class,
        JwtAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class NewEndpointAccessTest {

    private static final long CHILD_USER_ID = 101L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @MockBean
    private RobotControlService robotControlService;

    @MockBean
    private ChildFallAlertService childFallAlertService;

    @MockBean
    private NavigationTaskService navigationTaskService;

    @MockBean
    private EntertainmentService entertainmentService;

    @MockBean
    private VoiceCommandService voiceCommandService;

    @MockBean
    private InspectionMarkerService inspectionMarkerService;

    @Test
    void robotWrite_withoutToken_reachesController() throws Exception {
        when(robotControlService.sendCommand(any(RobotControlCommandRequest.class)))
                .thenReturn(InspectionApiResponse.ok(new RobotControlCommandResult("stop", null)));

        mockMvc.perform(post("/api/robot/control/command").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cmd\":\"stop\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cmd").value("stop"));

        verify(robotControlService).sendCommand(any(RobotControlCommandRequest.class));
    }

    @Test
    void fallAlerts_withoutToken_usesRequestedUserId() throws Exception {
        when(childFallAlertService.listFallAlerts(CHILD_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/child/fall-alerts").contextPath("/api")
                        .param("childUserId", Long.toString(CHILD_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(childFallAlertService).listFallAlerts(eq(CHILD_USER_ID));
    }

    @Test
    void health_withoutToken_isAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/health").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("ok"));
    }

    @Test
    void targetControllers_doNotEmbedContextPathInMappings() {
        Set<Class<?>> targetControllers = Set.of(
                ChildFallAlertController.class,
                RobotControlController.class,
                NavigationTaskController.class,
                EntertainmentController.class,
                VoiceCommandController.class,
                InspectionMarkerController.class);

        Set<String> patterns = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> targetControllers.contains(entry.getValue().getBeanType()))
                .flatMap(entry -> entry.getKey().getPatternValues().stream())
                .collect(Collectors.toSet());

        assertThat(patterns).isNotEmpty();
        assertThat(patterns).noneMatch(pattern -> pattern.startsWith("/api/"));

        assertThat(patternsFor(EntertainmentController.class))
                .contains("/entertainment/music")
                .doesNotContain("/api/entertainment/music", "/api/api/entertainment/music");
        assertThat(patternsFor(RobotControlController.class))
                .contains("/robot/control/command")
                .doesNotContain("/api/robot/control/command", "/api/api/robot/control/command");
        assertThat(patternsFor(VoiceCommandController.class))
                .contains("/voice/command")
                .doesNotContain("/api/voice/command", "/api/api/voice/command");
    }

    private Set<String> patternsFor(Class<?> controllerType) {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> controllerType.equals(entry.getValue().getBeanType()))
                .flatMap(entry -> entry.getKey().getPatternValues().stream())
                .collect(Collectors.toSet());
    }
}
