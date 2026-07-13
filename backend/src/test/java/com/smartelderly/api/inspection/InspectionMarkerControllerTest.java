package com.smartelderly.api.inspection;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InspectionMarkerControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RobotMapMarkerRepository repository;

    @BeforeEach
    void setUp() {
        authenticate(9001L);
        objectMapper = new ObjectMapper();
        repository = org.mockito.Mockito.mock(RobotMapMarkerRepository.class);
        InspectionMarkerService service = new InspectionMarkerService(repository);
        mockMvc = MockMvcBuilders.standaloneSetup(new InspectionMarkerController(service)).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMap_shouldReturnMockMapInfo() throws Exception {
        mockMvc.perform(apiGet("/api/inspection/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.mapId").value("floor1"))
                .andExpect(jsonPath("$.data.mapName").value("\u517b\u8001\u9662\u4e00\u5c42"))
                .andExpect(jsonPath("$.data.mapImage").value("assets/robot_maps/yahboomcar.png"))
                .andExpect(jsonPath("$.data.width").value(608))
                .andExpect(jsonPath("$.data.height").value(384))
                .andExpect(jsonPath("$.data.resolution").value(0.05))
                .andExpect(jsonPath("$.data.originX").value(-10.0))
                .andExpect(jsonPath("$.data.originY").value(-10.0))
                .andExpect(jsonPath("$.data.imageHeight").value(384));
    }

    @Test
    void getMarkers_shouldReadMarkersFromRepository() throws Exception {
        RobotMapMarker fallMarker = markerRow(1L, "fall", "张爷爷疑似跌倒", 120.0, 240.0, "danger", "unhandled");
        fallMarker.setDescription("张爷爷疑似在走廊区域跌倒");
        fallMarker.setLocationName("一层东侧走廊");
        fallMarker.setImageUrl("/static/mock/fall_001.jpg");
        fallMarker.setElderProfileId(42L);
        fallMarker.setElderName("张爷爷");
        fallMarker.setIdentitySource("recent_identity");
        fallMarker.setIdentityConfidence(0.89);
        fallMarker.setNotifiedChild(true);

        RobotMapMarker robotMarker = markerRow(4L, "robot", "小车当前位置", 260.0, 300.0, "info", "active");
        robotMarker.setNavigationStatus("running");
        robotMarker.setObstacleStatus("safe");
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(fallMarker, robotMarker));

        mockMvc.perform(apiGet("/api/inspection/markers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].type").value("fall"))
                .andExpect(jsonPath("$.data[0].title").value("张爷爷疑似跌倒"))
                .andExpect(jsonPath("$.data[0].message").value("张爷爷疑似在走廊区域跌倒"))
                .andExpect(jsonPath("$.data[0].description").value("张爷爷疑似在走廊区域跌倒"))
                .andExpect(jsonPath("$.data[0].x").value(120.0))
                .andExpect(jsonPath("$.data[0].y").value(240.0))
                .andExpect(jsonPath("$.data[0].level").value("danger"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("/static/mock/fall_001.jpg"))
                .andExpect(jsonPath("$.data[0].time").value("2026-07-10 16:30"))
                .andExpect(jsonPath("$.data[0].status").value("unhandled"))
                .andExpect(jsonPath("$.data[0].locationName").value("一层东侧走廊"))
                .andExpect(jsonPath("$.data[0].elderId").value(42))
                .andExpect(jsonPath("$.data[0].elderName").value("张爷爷"))
                .andExpect(jsonPath("$.data[0].identitySource").value("recent_identity"))
                .andExpect(jsonPath("$.data[0].identityConfidence").value(0.89))
                .andExpect(jsonPath("$.data[0].notifiedChild").value(true))
                .andExpect(jsonPath("$.data[0].source").value("mock"))
                .andExpect(jsonPath("$.data[1].id").value(4))
                .andExpect(jsonPath("$.data[1].type").value("robot"))
                .andExpect(jsonPath("$.data[1].navigationStatus").value("running"))
                .andExpect(jsonPath("$.data[1].obstacleStatus").value("safe"));
    }

    @Test
    void getMarker_shouldReturnMarkerDetailById() throws Exception {
        RobotMapMarker marker = markerRow(1L, "fall", "张爷爷疑似跌倒", 120.0, 240.0, "danger", "unhandled");
        marker.setLocationName("一层东侧走廊");
        marker.setDescription("摄像头检测到老人跌倒");
        marker.setElderProfileId(42L);
        marker.setElderName("张爷爷");
        marker.setIdentitySource("recent_identity");
        marker.setIdentityConfidence(0.89);
        marker.setNotifiedChild(true);
        when(repository.findById(1L)).thenReturn(Optional.of(marker));

        mockMvc.perform(apiGet("/api/inspection/markers/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.type").value("fall"))
                .andExpect(jsonPath("$.data.title").value("张爷爷疑似跌倒"))
                .andExpect(jsonPath("$.data.message").value("摄像头检测到老人跌倒"))
                .andExpect(jsonPath("$.data.description").value("摄像头检测到老人跌倒"))
                .andExpect(jsonPath("$.data.elderId").value(42))
                .andExpect(jsonPath("$.data.elderName").value("张爷爷"))
                .andExpect(jsonPath("$.data.identitySource").value("recent_identity"))
                .andExpect(jsonPath("$.data.identityConfidence").value(0.89))
                .andExpect(jsonPath("$.data.notifiedChild").value(true));
    }

    @Test
    void getMarker_shouldReturnUnknownFallDetailById() throws Exception {
        RobotMapMarker marker = markerRow(2L, "fall", "未知人员疑似跌倒", 300.0, 180.0, "danger", "unhandled");
        marker.setElderName("未知人员");
        marker.setIdentitySource("unknown");
        marker.setIdentityConfidence(0.0);
        marker.setNotifiedChild(false);
        when(repository.findById(2L)).thenReturn(Optional.of(marker));

        mockMvc.perform(apiGet("/api/inspection/markers/{id}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.type").value("fall"))
                .andExpect(jsonPath("$.data.title").value("未知人员疑似跌倒"))
                .andExpect(jsonPath("$.data.elderName").value("未知人员"))
                .andExpect(jsonPath("$.data.identitySource").value("unknown"))
                .andExpect(jsonPath("$.data.identityConfidence").value(0.0))
                .andExpect(jsonPath("$.data.notifiedChild").value(false));
    }

    @Test
    void getMarker_shouldReturnRobotStatusById() throws Exception {
        RobotMapMarker marker = markerRow(4L, "robot", "小车当前位置", 260.0, 300.0, "info", "active");
        marker.setNavigationStatus("running");
        marker.setObstacleStatus("safe");
        when(repository.findById(4L)).thenReturn(Optional.of(marker));

        mockMvc.perform(apiGet("/api/inspection/markers/{id}", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.type").value("robot"))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.navigationStatus").value("running"))
                .andExpect(jsonPath("$.data.obstacleStatus").value("safe"));
    }

    @Test
    void getMarker_shouldReturnHandledFallDetailById() throws Exception {
        RobotMapMarker marker = markerRow(7L, "fall", "李奶奶跌倒已处理", 650.0, 420.0, "danger", "handled");
        marker.setLocationName("一层西侧活动室");
        marker.setHandledByName("员工B");
        marker.setHandleRemark("已确认无明显外伤，安排休息观察");
        marker.setHandledAt(LocalDateTime.of(2026, 7, 10, 17, 8, 30));
        when(repository.findById(7L)).thenReturn(Optional.of(marker));

        mockMvc.perform(apiGet("/api/inspection/markers/{id}", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.type").value("fall"))
                .andExpect(jsonPath("$.data.title").value("李奶奶跌倒已处理"))
                .andExpect(jsonPath("$.data.status").value("handled"))
                .andExpect(jsonPath("$.data.locationName").value("一层西侧活动室"))
                .andExpect(jsonPath("$.data.handler").value("员工B"))
                .andExpect(jsonPath("$.data.remark").value("已确认无明显外伤，安排休息观察"));
    }

    @Test
    void createMarker_shouldSaveMarkerToRepository() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("type", "crack");
        request.put("title", "地面裂缝");
        request.put("description", "入口处发现裂缝");
        request.put("x", 360);
        request.put("y", 180);
        request.put("level", "warning");
        request.put("status", "unhandled");
        request.put("elderProfileId", 42);
        request.put("locationName", "一层大厅入口");
        request.put("source", "crack_detect");
        request.put("imageUrl", "/static/mock/crack_001.jpg");
        request.put("time", "2026-07-10 16:45");
        request.put("payloadJson", "{\"camera\":\"front\"}");
        when(repository.save(any(RobotMapMarker.class))).thenAnswer(invocation -> {
            RobotMapMarker saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        mockMvc.perform(post("/api/inspection/markers").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.type").value("crack"))
                .andExpect(jsonPath("$.data.message").value("入口处发现裂缝"))
                .andExpect(jsonPath("$.data.description").value("入口处发现裂缝"))
                .andExpect(jsonPath("$.data.source").value("crack_detect"))
                .andExpect(jsonPath("$.data.payloadJson").value("{\"camera\":\"front\"}"));

        ArgumentCaptor<RobotMapMarker> captor = ArgumentCaptor.forClass(RobotMapMarker.class);
        verify(repository).save(captor.capture());
        RobotMapMarker saved = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1L, saved.getMapId());
        org.junit.jupiter.api.Assertions.assertEquals(42L, saved.getElderProfileId());
        org.junit.jupiter.api.Assertions.assertEquals("crack", saved.getMarkerType());
        org.junit.jupiter.api.Assertions.assertEquals(360.0, saved.getLocationX());
        org.junit.jupiter.api.Assertions.assertEquals("一层大厅入口", saved.getLocationName());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2026, 7, 10, 16, 45), saved.getEventTime());
        org.junit.jupiter.api.Assertions.assertEquals(9001L, saved.getCreatedBy());
    }

    @Test
    void getMarker_shouldReturnFailureWhenMarkerMissing() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(apiGet("/api/inspection/markers/{id}", 999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("marker not found"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void handleMarker_shouldUpdateMarkerAndKeepStateForNextRead() throws Exception {
        Map<String, String> request = Map.of(
                "handler", "员工A",
                "remark", "已前往现场确认");
        RobotMapMarker marker = markerRow(1L, "fall", "张爷爷疑似跌倒", 120.0, 240.0, "danger", "unhandled");
        when(repository.findById(1L)).thenReturn(Optional.of(marker));
        when(repository.save(any(RobotMapMarker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(apiPut("/api/inspection/markers/{id}/handle", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("handled"))
                .andExpect(jsonPath("$.data.handler").value("员工A"))
                .andExpect(jsonPath("$.data.remark").value("已前往现场确认"))
                .andExpect(jsonPath("$.data.handleTime").isNotEmpty());

        org.junit.jupiter.api.Assertions.assertEquals("handled", marker.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(9001L, marker.getHandledBy());
        org.junit.jupiter.api.Assertions.assertEquals("员工A", marker.getHandledByName());
        org.junit.jupiter.api.Assertions.assertEquals("已前往现场确认", marker.getHandleRemark());
        org.junit.jupiter.api.Assertions.assertNotNull(marker.getHandledAt());

        mockMvc.perform(apiGet("/api/inspection/markers/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("handled"))
                .andExpect(jsonPath("$.data.handler").value("员工A"))
                .andExpect(jsonPath("$.data.remark").value("已前往现场确认"));
    }

    @Test
    void handleMarker_shouldReturnFailureWhenMarkerMissing() throws Exception {
        Map<String, String> request = Map.of(
                "handler", "员工A",
                "remark", "已前往现场确认");
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(apiPut("/api/inspection/markers/{id}/handle", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("marker not found"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private static RequestBuilder apiGet(String urlTemplate, Object... uriVariables) {
        return get(urlTemplate, uriVariables).contextPath("/api");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder apiPut(
            String urlTemplate,
            Object... uriVariables) {
        return put(urlTemplate, uriVariables).contextPath("/api");
    }

    private static RobotMapMarker markerRow(
            Long id,
            String markerType,
            String title,
            Double x,
            Double y,
            String level,
            String status) {
        RobotMapMarker marker = new RobotMapMarker();
        marker.setId(id);
        marker.setMapId(1L);
        marker.setMarkerType(markerType);
        marker.setTitle(title);
        marker.setLocationX(x);
        marker.setLocationY(y);
        marker.setLevel(level);
        marker.setStatus(status);
        marker.setSource("mock");
        marker.setEventTime(LocalDateTime.of(2026, 7, 10, 16, 30));
        return marker;
    }

    private static void authenticate(long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId, UserRole.child),
                null,
                AuthorityUtils.createAuthorityList("ROLE_child")));
    }
}
