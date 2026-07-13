package com.smartelderly.api.navigation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.GlobalExceptionHandler;
import com.smartelderly.api.inspection.RobotMapMarker;
import com.smartelderly.api.inspection.RobotMapMarkerRepository;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.AuthPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NavigationTaskControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RobotNavigationTaskRepository taskRepository;
    private RobotStatusRepository statusRepository;
    private RobotMapMarkerRepository markerRepository;

    @BeforeEach
    void setUp() {
        authenticate(9001L);
        objectMapper = new ObjectMapper();
        taskRepository = org.mockito.Mockito.mock(RobotNavigationTaskRepository.class);
        statusRepository = org.mockito.Mockito.mock(RobotStatusRepository.class);
        markerRepository = org.mockito.Mockito.mock(RobotMapMarkerRepository.class);
        NavigationTaskService service = new NavigationTaskService(taskRepository, statusRepository, markerRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new NavigationTaskController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTask_shouldCreateRunningTaskTargetMarkerAndRobotStatus() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("robotId", 1);
        request.put("creatorId", 9001);
        request.put("mapId", 1);
        request.put("targetName", "staff_target");
        request.put("targetX", 420);
        request.put("targetY", 210);
        when(taskRepository.save(any(RobotNavigationTask.class))).thenAnswer(invocation -> {
            RobotNavigationTask task = invocation.getArgument(0);
            task.setId(88L);
            return task;
        });
        when(statusRepository.findByRobotId(1L)).thenReturn(Optional.empty());
        when(statusRepository.save(any(RobotStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(markerRepository.findActiveTargetMarker(1L, 1L)).thenReturn(Optional.empty());
        when(markerRepository.save(any(RobotMapMarker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/navigation/tasks").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(88))
                .andExpect(jsonPath("$.data.status").value("running"))
                .andExpect(jsonPath("$.data.targetX").value(420.0))
                .andExpect(jsonPath("$.data.targetY").value(210.0))
                .andExpect(jsonPath("$.data.targetName").value("staff_target"));

        ArgumentCaptor<RobotNavigationTask> taskCaptor = ArgumentCaptor.forClass(RobotNavigationTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("running", taskCaptor.getValue().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(9001L, taskCaptor.getValue().getCreatorId());

        ArgumentCaptor<RobotMapMarker> markerCaptor = ArgumentCaptor.forClass(RobotMapMarker.class);
        verify(markerRepository).save(markerCaptor.capture());
        RobotMapMarker marker = markerCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("target", marker.getMarkerType());
        org.junit.jupiter.api.Assertions.assertEquals("active", marker.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("staff_target", marker.getTitle());
        org.junit.jupiter.api.Assertions.assertEquals(420.0, marker.getLocationX());
        org.junit.jupiter.api.Assertions.assertEquals(9001L, marker.getCreatedBy());

        ArgumentCaptor<RobotStatus> statusCaptor = ArgumentCaptor.forClass(RobotStatus.class);
        verify(statusRepository).save(statusCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("running", statusCaptor.getValue().getNavStatus());
        org.junit.jupiter.api.Assertions.assertEquals(100.0, statusCaptor.getValue().getCurrentX());
        org.junit.jupiter.api.Assertions.assertEquals(120.0, statusCaptor.getValue().getCurrentY());
    }

    @Test
    void status_shouldReturnRobotStatusAndLatestRunningTask() throws Exception {
        RobotStatus status = new RobotStatus();
        status.setRobotId(1L);
        status.setNavStatus("running");
        status.setCurrentX(100.0);
        status.setCurrentY(120.0);
        status.setLastMessage("navigating to elder room");
        RobotNavigationTask task = task(88L, 1L, "running", "elder_room", 420.0, 210.0);
        when(statusRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(status));
        when(taskRepository.findLatestActiveTask(1L)).thenReturn(Optional.of(task));

        mockMvc.perform(get("/api/navigation/status").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.navigationStatus").value("running"))
                .andExpect(jsonPath("$.data.currentX").value(100.0))
                .andExpect(jsonPath("$.data.currentY").value(120.0))
                .andExpect(jsonPath("$.data.targetX").value(420.0))
                .andExpect(jsonPath("$.data.targetY").value(210.0))
                .andExpect(jsonPath("$.data.targetName").value("elder_room"))
                .andExpect(jsonPath("$.data.message").value("navigating to elder room"));
    }

    @Test
    void cancelTask_shouldCancelTaskAndPauseRobotStatus() throws Exception {
        RobotNavigationTask task = task(88L, 1L, "running", "elder_room", 420.0, 210.0);
        RobotStatus status = new RobotStatus();
        status.setRobotId(1L);
        status.setNavStatus("running");
        when(taskRepository.findById(88L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(RobotNavigationTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statusRepository.findByRobotId(1L)).thenReturn(Optional.of(status));
        when(statusRepository.save(any(RobotStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/navigation/tasks/{id}/cancel", 88).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(88))
                .andExpect(jsonPath("$.data.status").value("cancelled"))
                .andExpect(jsonPath("$.data.targetName").value("elder_room"));

        org.junit.jupiter.api.Assertions.assertEquals("cancelled", task.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("paused", status.getNavStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(task.getFinishedAt());
    }

    @Test
    void cancelTask_shouldRejectDifferentCreator() throws Exception {
        authenticate(9002L);
        RobotNavigationTask task = task(88L, 1L, "running", "elder_room", 420.0, 210.0);
        when(taskRepository.findById(88L)).thenReturn(Optional.of(task));

        mockMvc.perform(post("/api/navigation/tasks/{id}/cancel", 88).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4030));

        verify(taskRepository, never()).save(any(RobotNavigationTask.class));
        verify(statusRepository, never()).save(any(RobotStatus.class));
    }

    private static RobotNavigationTask task(
            Long id,
            Long robotId,
            String status,
            String targetName,
            Double targetX,
            Double targetY) {
        RobotNavigationTask task = new RobotNavigationTask();
        task.setId(id);
        task.setRobotId(robotId);
        task.setCreatorId(9001L);
        task.setStatus(status);
        task.setTargetName(targetName);
        task.setTargetX(targetX);
        task.setTargetY(targetY);
        return task;
    }

    private static void authenticate(long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId, UserRole.child),
                null,
                AuthorityUtils.createAuthorityList("ROLE_child")));
    }
}
