package com.smartelderly.api.voice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.entertainment.EntertainmentService;
import com.smartelderly.api.entertainment.RobotCommandLog;
import com.smartelderly.api.entertainment.RobotCommandLogRepository;
import com.smartelderly.api.entertainment.RobotEntertainmentTask;
import com.smartelderly.api.entertainment.RobotEntertainmentTaskRepository;
import com.smartelderly.api.entertainment.RobotMusicLibraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VoiceCommandControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RobotEntertainmentTaskRepository taskRepository;
    private RobotCommandLogRepository commandLogRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RobotMusicLibraryRepository musicRepository = org.mockito.Mockito.mock(RobotMusicLibraryRepository.class);
        taskRepository = org.mockito.Mockito.mock(RobotEntertainmentTaskRepository.class);
        commandLogRepository = org.mockito.Mockito.mock(RobotCommandLogRepository.class);
        EntertainmentService entertainmentService =
                new EntertainmentService(musicRepository, taskRepository, commandLogRepository, objectMapper);
        VoiceCommandService voiceCommandService =
                new VoiceCommandService(entertainmentService, commandLogRepository, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(new VoiceCommandController(voiceCommandService)).build();
    }

    @Test
    void command_shouldSupportPlayMusicAndCreateEntertainmentTask() throws Exception {
        when(taskRepository.save(any(RobotEntertainmentTask.class))).thenAnswer(invocation -> {
            RobotEntertainmentTask task = invocation.getArgument(0);
            task.setId(101L);
            return task;
        });
        when(commandLogRepository.save(any(RobotCommandLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/voice/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(playMusicRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.command").value("play_music"))
                .andExpect(jsonPath("$.data.taskId").value(101))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.feedback").value("play_music accepted"));

        ArgumentCaptor<RobotEntertainmentTask> taskCaptor = ArgumentCaptor.forClass(RobotEntertainmentTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("music", taskCaptor.getValue().getTaskType());

        ArgumentCaptor<RobotCommandLog> commandCaptor = ArgumentCaptor.forClass(RobotCommandLog.class);
        verify(commandLogRepository).save(commandCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("music", commandCaptor.getValue().getCommandType());
        org.junit.jupiter.api.Assertions.assertEquals("play_music", commandCaptor.getValue().getCommand());
        org.junit.jupiter.api.Assertions.assertEquals("sent", commandCaptor.getValue().getStatus());
    }

    @Test
    void command_shouldSupportDanceAndCreateEntertainmentTask() throws Exception {
        when(taskRepository.save(any(RobotEntertainmentTask.class))).thenAnswer(invocation -> {
            RobotEntertainmentTask task = invocation.getArgument(0);
            task.setId(102L);
            return task;
        });
        when(commandLogRepository.save(any(RobotCommandLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> request = playMusicRequest();
        request.put("command", "dance");
        request.put("danceMode", "gentle");
        mockMvc.perform(post("/api/voice/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.command").value("dance"))
                .andExpect(jsonPath("$.data.taskId").value(102))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.feedback").value("dance accepted"));

        ArgumentCaptor<RobotEntertainmentTask> taskCaptor = ArgumentCaptor.forClass(RobotEntertainmentTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("music_dance", taskCaptor.getValue().getTaskType());
        org.junit.jupiter.api.Assertions.assertEquals("gentle", taskCaptor.getValue().getDanceMode());
    }

    @Test
    void command_shouldSupportHelpWithoutWritingEntertainmentTask() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("command", "help");
        request.put("robotId", 1);
        request.put("userId", 9001);

        mockMvc.perform(post("/api/voice/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.command").value("help"))
                .andExpect(jsonPath("$.data.taskId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("accepted"))
                .andExpect(jsonPath("$.data.feedback").value("SOS/help accepted"));

        verify(taskRepository, never()).save(any(RobotEntertainmentTask.class));
        verify(commandLogRepository, never()).save(any(RobotCommandLog.class));
    }

    @Test
    void command_shouldSupportStopAndWriteCommandLog() throws Exception {
        when(commandLogRepository.save(any(RobotCommandLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("command", "stop");
        request.put("robotId", 1);
        request.put("userId", 9001);

        mockMvc.perform(post("/api/voice/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.command").value("stop"))
                .andExpect(jsonPath("$.data.taskId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("sent"))
                .andExpect(jsonPath("$.data.feedback").value("stop accepted"));

        ArgumentCaptor<RobotCommandLog> commandCaptor = ArgumentCaptor.forClass(RobotCommandLog.class);
        verify(commandLogRepository).save(commandCaptor.capture());
        RobotCommandLog command = commandCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("system", command.getCommandType());
        org.junit.jupiter.api.Assertions.assertEquals("stop", command.getCommand());
        org.junit.jupiter.api.Assertions.assertEquals("sent", command.getStatus());
    }

    private static Map<String, Object> playMusicRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("command", "play_music");
        request.put("robotId", 1);
        request.put("userId", 9001);
        request.put("musicId", 1);
        request.put("musicName", "小苹果");
        request.put("musicUrl", "/static/music/xiaopingguo.mp3");
        return request;
    }
}
