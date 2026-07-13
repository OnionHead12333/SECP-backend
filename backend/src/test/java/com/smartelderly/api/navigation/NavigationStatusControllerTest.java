package com.smartelderly.api.navigation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.smartelderly.api.inspection.RobotMapMarkerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NavigationStatusControllerTest {

    @Test
    void navigationStatus_shouldReturnFrontendMockStatus() throws Exception {
        RobotNavigationTaskRepository taskRepository = org.mockito.Mockito.mock(RobotNavigationTaskRepository.class);
        RobotStatusRepository statusRepository = org.mockito.Mockito.mock(RobotStatusRepository.class);
        RobotMapMarkerRepository markerRepository = org.mockito.Mockito.mock(RobotMapMarkerRepository.class);
        org.mockito.Mockito.when(statusRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        NavigationTaskService service = new NavigationTaskService(taskRepository, statusRepository, markerRepository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NavigationTaskController(service)).build();

        mockMvc.perform(get("/api/navigation/status").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.navigationStatus").value("running"))
                .andExpect(jsonPath("$.data.currentX").value(100))
                .andExpect(jsonPath("$.data.currentY").value(120))
                .andExpect(jsonPath("$.data.targetX").value(420))
                .andExpect(jsonPath("$.data.targetY").value(210))
                .andExpect(jsonPath("$.data.targetName").value("elder_room"))
                .andExpect(jsonPath("$.data.message").value("\u6b63\u5728\u524d\u5f80\u8001\u4eba\u623f\u95f4"));
    }
}
