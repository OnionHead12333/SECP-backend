package com.smartelderly.api.entertainment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntertainmentService {

    private static final String MOCK_ACCEPTED = "mock accepted";
    private static final int RECENT_TASK_LIMIT = 20;
    private static final DateTimeFormatter GATEWAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ALLOWED_TASK_STATUSES =
            Set.of("pending", "running", "completed", "failed", "cancelled");
    private static final Set<String> TERMINAL_TASK_STATUSES =
            Set.of("completed", "failed", "cancelled");

    private final RobotMusicLibraryRepository musicRepository;
    private final RobotEntertainmentTaskRepository taskRepository;
    private final RobotCommandLogRepository commandLogRepository;
    private final ObjectMapper objectMapper;

    public EntertainmentService(
            RobotMusicLibraryRepository musicRepository,
            RobotEntertainmentTaskRepository taskRepository,
            RobotCommandLogRepository commandLogRepository,
            ObjectMapper objectMapper) {
        this.musicRepository = musicRepository;
        this.taskRepository = taskRepository;
        this.commandLogRepository = commandLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<MusicResponse> listMusic() {
        return musicRepository.findByEnabledTrueOrderByIdAsc().stream()
                .map(this::toMusicResponse)
                .toList();
    }

    @Transactional
    public EntertainmentTaskResponse playMusic(EntertainmentCommandRequest request) {
        return createTaskAndCommand(request, "music", null, "music", "play_music");
    }

    @Transactional
    public EntertainmentTaskResponse startDance(EntertainmentCommandRequest request) {
        return createTaskAndCommand(request, "music_dance", request.danceMode(), "dance", "dance");
    }

    @Transactional(readOnly = true)
    public List<EntertainmentTaskResponse> listRecentTasks() {
        return taskRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_TASK_LIMIT)).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingEntertainmentTaskResponse> listPendingTasks() {
        return taskRepository.findByStatusOrderByCreatedAtAsc("pending").stream()
                .map(this::toPendingTaskResponse)
                .toList();
    }

    @Transactional
    public Optional<UpdateEntertainmentTaskStatusResponse> updateTaskStatus(
            long taskId,
            UpdateEntertainmentTaskStatusRequest request,
            long userId) {
        String status = normalizeStatus(request.status());
        if (!ALLOWED_TASK_STATUSES.contains(status)) {
            throw new IllegalArgumentException("unsupported entertainment task status: " + status);
        }
        return taskRepository.findById(taskId)
                .map(task -> {
                    if (!Objects.equals(task.getUserId(), userId)) {
                        throw new ApiException(4030, "forbidden");
                    }
                    LocalDateTime now = LocalDateTime.now();
                    task.setStatus(status);
                    task.setResponseMessage(request.message());
                    if ("running".equals(status)) {
                        task.setStartedAt(now);
                    }
                    if (TERMINAL_TASK_STATUSES.contains(status)) {
                        task.setFinishedAt(now);
                    }
                    RobotEntertainmentTask saved = taskRepository.save(task);
                    commandLogRepository.save(toGatewayCallbackLog(saved, request));
                    return new UpdateEntertainmentTaskStatusResponse(
                            saved.getId(),
                            saved.getStatus(),
                            saved.getResponseMessage());
                });
    }

    @Transactional(readOnly = true)
    public EntertainmentStatusResponse getLatestStatus() {
        return taskRepository.findFirstByOrderByCreatedAtDesc()
                .map(this::toStatusResponse)
                .orElse(null);
    }

    private EntertainmentTaskResponse createTaskAndCommand(
            EntertainmentCommandRequest request,
            String taskType,
            String danceMode,
            String commandType,
            String commandName) {
        String requestJson = toJson(request);

        RobotEntertainmentTask task = new RobotEntertainmentTask();
        task.setRobotId(request.robotId());
        task.setUserId(request.userId());
        task.setMusicId(request.musicId());
        task.setTaskType(taskType);
        task.setMusicName(request.musicName());
        task.setMusicUrl(request.musicUrl());
        task.setDanceMode(danceMode);
        task.setRequestJson(requestJson);
        task.setStatus("pending");
        task.setResponseMessage(MOCK_ACCEPTED);
        RobotEntertainmentTask savedTask = taskRepository.save(task);

        RobotCommandLog commandLog = new RobotCommandLog();
        commandLog.setRobotId(request.robotId());
        commandLog.setUserId(request.userId());
        commandLog.setCommandType(commandType);
        commandLog.setCommand(commandName);
        commandLog.setRequestJson(requestJson);
        commandLog.setStatus("sent");
        commandLog.setResponseMessage(MOCK_ACCEPTED);
        commandLogRepository.save(commandLog);

        return toTaskResponse(savedTask);
    }

    private MusicResponse toMusicResponse(RobotMusicLibrary music) {
        return new MusicResponse(
                music.getId(),
                music.getMusicName(),
                music.getMusicUrl(),
                music.getArtist(),
                music.getDurationSeconds(),
                music.getSuitableScene());
    }

    private EntertainmentTaskResponse toTaskResponse(RobotEntertainmentTask task) {
        return new EntertainmentTaskResponse(
                task.getId(),
                task.getRobotId(),
                task.getUserId(),
                task.getTaskType(),
                task.getMusicId(),
                task.getMusicName(),
                task.getMusicUrl(),
                task.getDanceMode(),
                task.getStatus(),
                task.getResponseMessage(),
                formatTime(task.getCreatedAt()),
                formatTime(task.getStartedAt()),
                formatTime(task.getFinishedAt()));
    }

    private PendingEntertainmentTaskResponse toPendingTaskResponse(RobotEntertainmentTask task) {
        return new PendingEntertainmentTaskResponse(
                task.getId(),
                task.getRobotId(),
                task.getUserId(),
                task.getTaskType(),
                task.getMusicId(),
                task.getMusicName(),
                task.getMusicUrl(),
                task.getDanceMode(),
                task.getStatus(),
                task.getResponseMessage(),
                formatGatewayTime(task.getCreatedAt()));
    }

    private EntertainmentStatusResponse toStatusResponse(RobotEntertainmentTask task) {
        return new EntertainmentStatusResponse(
                task.getId(),
                task.getTaskType(),
                task.getMusicName(),
                task.getDanceMode(),
                task.getStatus(),
                task.getResponseMessage());
    }

    private String toJson(EntertainmentCommandRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid entertainment request", e);
        }
    }

    private String toJson(UpdateEntertainmentTaskStatusRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid entertainment status request", e);
        }
    }

    private RobotCommandLog toGatewayCallbackLog(
            RobotEntertainmentTask task,
            UpdateEntertainmentTaskStatusRequest request) {
        RobotCommandLog commandLog = new RobotCommandLog();
        commandLog.setRobotId(task.getRobotId());
        commandLog.setUserId(task.getUserId());
        commandLog.setCommandType("system");
        commandLog.setCommand("entertainment_task_status");
        commandLog.setRequestJson(toJson(request));
        commandLog.setStatus(toCommandLogStatus(task.getStatus()));
        commandLog.setResponseMessage(request.message());
        return commandLog;
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private static String toCommandLogStatus(String taskStatus) {
        return switch (taskStatus) {
            case "pending" -> "pending";
            case "running" -> "sent";
            case "completed" -> "success";
            default -> "failed";
        };
    }

    private static String formatTime(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String formatGatewayTime(LocalDateTime value) {
        return value == null ? null : value.format(GATEWAY_TIME_FORMATTER);
    }
}
