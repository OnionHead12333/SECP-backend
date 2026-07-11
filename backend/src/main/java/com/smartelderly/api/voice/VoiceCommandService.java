package com.smartelderly.api.voice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.entertainment.EntertainmentCommandRequest;
import com.smartelderly.api.entertainment.EntertainmentService;
import com.smartelderly.api.entertainment.EntertainmentTaskResponse;
import com.smartelderly.api.entertainment.RobotCommandLog;
import com.smartelderly.api.entertainment.RobotCommandLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoiceCommandService {

    private final EntertainmentService entertainmentService;
    private final RobotCommandLogRepository commandLogRepository;
    private final ObjectMapper objectMapper;

    public VoiceCommandService(
            EntertainmentService entertainmentService,
            RobotCommandLogRepository commandLogRepository,
            ObjectMapper objectMapper) {
        this.entertainmentService = entertainmentService;
        this.commandLogRepository = commandLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VoiceCommandResponse handle(VoiceCommandRequest request) {
        String command = normalizeCommand(request.command());
        return switch (command) {
            case "play_music" -> playMusic(request);
            case "dance" -> dance(request);
            case "help" -> new VoiceCommandResponse(command, null, "accepted", "SOS/help accepted");
            case "stop" -> stop(request);
            default -> throw new IllegalArgumentException("unsupported voice command: " + command);
        };
    }

    private VoiceCommandResponse playMusic(VoiceCommandRequest request) {
        EntertainmentTaskResponse task = entertainmentService.playMusic(toEntertainmentRequest(request));
        return new VoiceCommandResponse("play_music", task.id(), task.status(), "play_music accepted");
    }

    private VoiceCommandResponse dance(VoiceCommandRequest request) {
        EntertainmentTaskResponse task = entertainmentService.startDance(toEntertainmentRequest(request));
        return new VoiceCommandResponse("dance", task.id(), task.status(), "dance accepted");
    }

    private VoiceCommandResponse stop(VoiceCommandRequest request) {
        RobotCommandLog commandLog = new RobotCommandLog();
        commandLog.setRobotId(request.robotId());
        commandLog.setUserId(request.userId());
        commandLog.setCommandType("system");
        commandLog.setCommand("stop");
        commandLog.setRequestJson(toJson(request));
        commandLog.setStatus("sent");
        commandLog.setResponseMessage("stop accepted");
        commandLogRepository.save(commandLog);
        return new VoiceCommandResponse("stop", null, "sent", "stop accepted");
    }

    private EntertainmentCommandRequest toEntertainmentRequest(VoiceCommandRequest request) {
        return new EntertainmentCommandRequest(
                request.robotId(),
                request.userId(),
                request.musicId(),
                request.musicName(),
                request.musicUrl(),
                request.danceMode());
    }

    private String toJson(VoiceCommandRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid voice command request", e);
        }
    }

    private static String normalizeCommand(String command) {
        return command == null ? "" : command.trim().toLowerCase();
    }
}
