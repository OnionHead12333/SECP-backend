package com.smartelderly.api.child;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.smartelderly.api.inspection.RobotMapMarker;
import com.smartelderly.api.inspection.RobotMapMarkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChildFallAlertControllerTest {

    private MockMvc mockMvc;
    private RobotMapMarkerRepository repository;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(RobotMapMarkerRepository.class);
        ChildFallAlertService service = new ChildFallAlertService(repository);
        mockMvc = MockMvcBuilders.standaloneSetup(new ChildFallAlertController(service)).build();
    }

    @Test
    void listFallAlerts_shouldReturnFallMarkersWithChildFields() throws Exception {
        RobotMapMarker unhandled = fallMarker(1L, "张爷爷疑似跌倒", "unhandled",
                LocalDateTime.of(2026, 7, 10, 16, 30));
        unhandled.setCreatedAt(LocalDateTime.of(2026, 7, 10, 16, 29));
        RobotMapMarker handled = fallMarker(2L, "李奶奶跌倒已处理", "handled",
                LocalDateTime.of(2026, 7, 10, 17, 2));
        handled.setHandledByName("员工B");
        handled.setHandleRemark("已确认无明显外伤");
        handled.setHandledAt(LocalDateTime.of(2026, 7, 10, 17, 8, 30));
        when(repository.findFallAlertsForChild(101L)).thenReturn(List.of(unhandled, handled));

        mockMvc.perform(get("/api/child/fall-alerts").contextPath("/api")
                        .param("childUserId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].type").value("fall"))
                .andExpect(jsonPath("$.data[0].title").value("张爷爷疑似跌倒"))
                .andExpect(jsonPath("$.data[0].message").value("张爷爷疑似在走廊区域跌倒"))
                .andExpect(jsonPath("$.data[0].description").value("张爷爷疑似在走廊区域跌倒"))
                .andExpect(jsonPath("$.data[0].elderName").value("张爷爷"))
                .andExpect(jsonPath("$.data[0].identitySource").value("recent_identity"))
                .andExpect(jsonPath("$.data[0].identityConfidence").value(0.89))
                .andExpect(jsonPath("$.data[0].notifiedChild").value(true))
                .andExpect(jsonPath("$.data[0].elderProfileId").value(42))
                .andExpect(jsonPath("$.data[0].locationName").value("一层东侧走廊"))
                .andExpect(jsonPath("$.data[0].x").value(120.0))
                .andExpect(jsonPath("$.data[0].y").value(240.0))
                .andExpect(jsonPath("$.data[0].level").value("danger"))
                .andExpect(jsonPath("$.data[0].status").value("unhandled"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("/static/mock/fall_001.jpg"))
                .andExpect(jsonPath("$.data[0].time").value("2026-07-10 16:30"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].status").value("handled"))
                .andExpect(jsonPath("$.data[1].handler").value("员工B"))
                .andExpect(jsonPath("$.data[1].remark").value("已确认无明显外伤"))
                .andExpect(jsonPath("$.data[1].handleTime").value("2026-07-10 17:08:30"));
    }

    @Test
    void getFallAlert_shouldReturnDetailById() throws Exception {
        RobotMapMarker marker = fallMarker(1L, "张爷爷疑似跌倒", "unhandled",
                LocalDateTime.of(2026, 7, 10, 16, 30));
        when(repository.findFallAlertByIdAndChildUserId(1L, 101L)).thenReturn(Optional.of(marker));

        mockMvc.perform(get("/api/child/fall-alerts/{id}", 1).contextPath("/api")
                        .param("childUserId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.elderProfileId").value(42))
                .andExpect(jsonPath("$.data.title").value("张爷爷疑似跌倒"))
                .andExpect(jsonPath("$.data.message").value("张爷爷疑似在走廊区域跌倒"))
                .andExpect(jsonPath("$.data.description").value("张爷爷疑似在走廊区域跌倒"));
    }

    @Test
    void getFallAlert_shouldReturnFailureWhenMissing() throws Exception {
        when(repository.findFallAlertByIdAndChildUserId(999L, 101L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/child/fall-alerts/{id}", 999).contextPath("/api")
                        .param("childUserId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("fall alert not found"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private static RobotMapMarker fallMarker(Long id, String title, String status, LocalDateTime eventTime) {
        RobotMapMarker marker = new RobotMapMarker();
        marker.setId(id);
        marker.setMapId(1L);
        marker.setElderProfileId(42L);
        marker.setMarkerType("fall");
        marker.setTitle(title);
        marker.setDescription("张爷爷疑似在走廊区域跌倒");
        marker.setLocationName("一层东侧走廊");
        marker.setLocationX(120.0);
        marker.setLocationY(240.0);
        marker.setLevel("danger");
        marker.setStatus(status);
        marker.setImageUrl("/static/mock/fall_001.jpg");
        marker.setEventTime(eventTime);
        marker.setElderName("张爷爷");
        marker.setIdentitySource("recent_identity");
        marker.setIdentityConfidence(0.89);
        marker.setNotifiedChild(true);
        return marker;
    }
}
