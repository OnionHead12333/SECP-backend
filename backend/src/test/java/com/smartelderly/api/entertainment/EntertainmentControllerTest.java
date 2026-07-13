package com.smartelderly.api.entertainment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.NoHandlerFoundException;

class EntertainmentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RobotMusicLibraryRepository musicRepository;
    private RobotEntertainmentTaskRepository taskRepository;
    private RobotCommandLogRepository commandLogRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        musicRepository = org.mockito.Mockito.mock(RobotMusicLibraryRepository.class);
        taskRepository = org.mockito.Mockito.mock(RobotEntertainmentTaskRepository.class);
        commandLogRepository = org.mockito.Mockito.mock(RobotCommandLogRepository.class);
        EntertainmentService service =
                new EntertainmentService(musicRepository, taskRepository, commandLogRepository, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(new EntertainmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void music_shouldReturnEnabledMusicList() throws Exception {
        RobotMusicLibrary music = new RobotMusicLibrary();
        music.setId(1L);
        music.setMusicName("小苹果");
        music.setMusicUrl("/static/music/xiaopingguo.mp3");
        music.setArtist("筷子兄弟");
        music.setDurationSeconds(210);
        music.setSuitableScene("dance");
        music.setEnabled(true);
        when(musicRepository.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(music));

        mockMvc.perform(get("/api/entertainment/music").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].musicName").value("小苹果"))
                .andExpect(jsonPath("$.data[0].musicUrl").value("/static/music/xiaopingguo.mp3"))
                .andExpect(jsonPath("$.data[0].artist").value("筷子兄弟"))
                .andExpect(jsonPath("$.data[0].durationSeconds").value(210))
                .andExpect(jsonPath("$.data[0].suitableScene").value("dance"));
    }

    @Test
    void music_shouldNotRegisterContextPathTwice() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/api/entertainment/music").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5000))
                .andReturn();

        assertThat(result.getRequest().getRequestURI()).isEqualTo("/api/api/entertainment/music");
        assertThat(result.getRequest().getContextPath()).isEqualTo("/api");
        assertThat(result.getRequest().getServletPath()).isEmpty();
        assertThat(result.getHandler()).isNull();
        assertThat(result.getResolvedException()).isInstanceOf(NoHandlerFoundException.class);
        verifyNoInteractions(musicRepository, taskRepository, commandLogRepository);
    }

    @Test
    void playMusic_shouldCreatePendingTaskAndSentCommandLog() throws Exception {
        when(taskRepository.save(any(RobotEntertainmentTask.class))).thenAnswer(invocation -> {
            RobotEntertainmentTask saved = invocation.getArgument(0);
            saved.setId(11L);
            saved.setCreatedAt(LocalDateTime.of(2026, 7, 11, 10, 0));
            return saved;
        });
        when(commandLogRepository.save(any(RobotCommandLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> request = musicRequest();
        mockMvc.perform(post("/api/entertainment/music/play").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.robotId").value(1))
                .andExpect(jsonPath("$.data.userId").value(9001))
                .andExpect(jsonPath("$.data.taskType").value("music"))
                .andExpect(jsonPath("$.data.musicId").value(1))
                .andExpect(jsonPath("$.data.musicName").value("小苹果"))
                .andExpect(jsonPath("$.data.musicUrl").value("/static/music/xiaopingguo.mp3"))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.responseMessage").value("mock accepted"));

        ArgumentCaptor<RobotEntertainmentTask> taskCaptor = ArgumentCaptor.forClass(RobotEntertainmentTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        RobotEntertainmentTask task = taskCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("music", task.getTaskType());
        org.junit.jupiter.api.Assertions.assertEquals("pending", task.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("mock accepted", task.getResponseMessage());
        org.junit.jupiter.api.Assertions.assertTrue(task.getRequestJson().contains("\"robotId\":1"));

        ArgumentCaptor<RobotCommandLog> commandCaptor = ArgumentCaptor.forClass(RobotCommandLog.class);
        verify(commandLogRepository).save(commandCaptor.capture());
        RobotCommandLog command = commandCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("music", command.getCommandType());
        org.junit.jupiter.api.Assertions.assertEquals("play_music", command.getCommand());
        org.junit.jupiter.api.Assertions.assertEquals("sent", command.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("mock accepted", command.getResponseMessage());
    }

    @Test
    void startDance_shouldCreateMusicDanceTaskAndDanceCommandLog() throws Exception {
        when(taskRepository.save(any(RobotEntertainmentTask.class))).thenAnswer(invocation -> {
            RobotEntertainmentTask saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });
        when(commandLogRepository.save(any(RobotCommandLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> request = musicRequest();
        request.put("danceMode", "gentle");
        mockMvc.perform(post("/api/entertainment/dance/start").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.taskType").value("music_dance"))
                .andExpect(jsonPath("$.data.danceMode").value("gentle"))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.responseMessage").value("mock accepted"));

        ArgumentCaptor<RobotCommandLog> commandCaptor = ArgumentCaptor.forClass(RobotCommandLog.class);
        verify(commandLogRepository).save(commandCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("dance", commandCaptor.getValue().getCommandType());
        org.junit.jupiter.api.Assertions.assertEquals("dance", commandCaptor.getValue().getCommand());
    }

    @Test
    void tasks_shouldReturnRecentEntertainmentTasks() throws Exception {
        RobotEntertainmentTask task = taskRow(21L);
        when(taskRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20))).thenReturn(List.of(task));

        mockMvc.perform(get("/api/entertainment/tasks").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(21))
                .andExpect(jsonPath("$.data[0].robotId").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(9001))
                .andExpect(jsonPath("$.data[0].taskType").value("music_dance"))
                .andExpect(jsonPath("$.data[0].musicId").value(1))
                .andExpect(jsonPath("$.data[0].musicName").value("小苹果"))
                .andExpect(jsonPath("$.data[0].musicUrl").value("/static/music/xiaopingguo.mp3"))
                .andExpect(jsonPath("$.data[0].danceMode").value("gentle"))
                .andExpect(jsonPath("$.data[0].status").value("pending"))
                .andExpect(jsonPath("$.data[0].responseMessage").value("mock accepted"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-07-11T10:00:00"))
                .andExpect(jsonPath("$.data[0].startedAt").value("2026-07-11T10:01:00"))
                .andExpect(jsonPath("$.data[0].finishedAt").value("2026-07-11T10:02:00"));
    }

    @Test
    void pendingTasks_shouldReturnPendingTasksForGatewayInCreatedOrder() throws Exception {
        RobotEntertainmentTask task = taskRow(22L);
        when(taskRepository.findByStatusOrderByCreatedAtAsc("pending")).thenReturn(List.of(task));

        mockMvc.perform(get("/api/entertainment/tasks/pending").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].taskId").value(22))
                .andExpect(jsonPath("$.data[0].robotId").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(9001))
                .andExpect(jsonPath("$.data[0].taskType").value("music_dance"))
                .andExpect(jsonPath("$.data[0].musicId").value(1))
                .andExpect(jsonPath("$.data[0].musicName").value("小苹果"))
                .andExpect(jsonPath("$.data[0].musicUrl").value("/static/music/xiaopingguo.mp3"))
                .andExpect(jsonPath("$.data[0].danceMode").value("gentle"))
                .andExpect(jsonPath("$.data[0].status").value("pending"))
                .andExpect(jsonPath("$.data[0].responseMessage").value("mock accepted"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-07-11 10:00:00"));
    }

    @Test
    void updateTaskStatus_shouldUpdateTaskAndWriteGatewayCallbackLog() throws Exception {
        RobotEntertainmentTask task = taskRow(23L);
        task.setFinishedAt(null);
        when(taskRepository.findById(23L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(RobotEntertainmentTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commandLogRepository.save(any(RobotCommandLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("status", "running");
        request.put("message", "A 网关模拟：小车开始跳舞");
        mockMvc.perform(put("/api/entertainment/tasks/{taskId}/status", 23).contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("任务状态已更新"))
                .andExpect(jsonPath("$.data.taskId").value(23))
                .andExpect(jsonPath("$.data.status").value("running"))
                .andExpect(jsonPath("$.data.responseMessage").value("A 网关模拟：小车开始跳舞"));

        org.junit.jupiter.api.Assertions.assertEquals("running", task.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("A 网关模拟：小车开始跳舞", task.getResponseMessage());
        org.junit.jupiter.api.Assertions.assertNotNull(task.getStartedAt());
        org.junit.jupiter.api.Assertions.assertNull(task.getFinishedAt());

        ArgumentCaptor<RobotCommandLog> commandCaptor = ArgumentCaptor.forClass(RobotCommandLog.class);
        verify(commandLogRepository).save(commandCaptor.capture());
        RobotCommandLog command = commandCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("system", command.getCommandType());
        org.junit.jupiter.api.Assertions.assertEquals("entertainment_task_status", command.getCommand());
        org.junit.jupiter.api.Assertions.assertEquals("sent", command.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("A 网关模拟：小车开始跳舞", command.getResponseMessage());
    }

    @Test
    void updateTaskStatus_shouldSetFinishedAtForTerminalStatus() throws Exception {
        RobotEntertainmentTask task = taskRow(24L);
        when(taskRepository.findById(24L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(RobotEntertainmentTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commandLogRepository.save(any(RobotCommandLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("status", "completed");
        request.put("message", "A 网关模拟：跳舞完成");
        mockMvc.perform(put("/api/entertainment/tasks/{taskId}/status", 24).contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(24))
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.responseMessage").value("A 网关模拟：跳舞完成"));

        org.junit.jupiter.api.Assertions.assertEquals("completed", task.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(task.getFinishedAt());
    }

    @Test
    void status_shouldReturnLatestEntertainmentTaskStatus() throws Exception {
        when(taskRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(taskRow(21L)));

        mockMvc.perform(get("/api/entertainment/status").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentTaskId").value(21))
                .andExpect(jsonPath("$.data.taskType").value("music_dance"))
                .andExpect(jsonPath("$.data.musicName").value("小苹果"))
                .andExpect(jsonPath("$.data.danceMode").value("gentle"))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.responseMessage").value("mock accepted"));
    }

    @Test
    void status_shouldReturnNullWhenNoTaskExists() throws Exception {
        when(taskRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/entertainment/status").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private static Map<String, Object> musicRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("robotId", 1);
        request.put("userId", 9001);
        request.put("musicId", 1);
        request.put("musicName", "小苹果");
        request.put("musicUrl", "/static/music/xiaopingguo.mp3");
        return request;
    }

    private static RobotEntertainmentTask taskRow(Long id) {
        RobotEntertainmentTask task = new RobotEntertainmentTask();
        task.setId(id);
        task.setRobotId(1L);
        task.setUserId(9001L);
        task.setTaskType("music_dance");
        task.setMusicId(1L);
        task.setMusicName("小苹果");
        task.setMusicUrl("/static/music/xiaopingguo.mp3");
        task.setDanceMode("gentle");
        task.setStatus("pending");
        task.setResponseMessage("mock accepted");
        task.setCreatedAt(LocalDateTime.of(2026, 7, 11, 10, 0));
        task.setStartedAt(LocalDateTime.of(2026, 7, 11, 10, 1));
        task.setFinishedAt(LocalDateTime.of(2026, 7, 11, 10, 2));
        return task;
    }
}
