package com.smartelderly.api.navigation;

import java.time.LocalDateTime;
import java.util.Optional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.inspection.RobotMapMarker;
import com.smartelderly.api.inspection.RobotMapMarkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavigationTaskService {

    private static final long DEFAULT_MAP_ID = 1L;
    private static final NavigationStatusResponse DEFAULT_STATUS = new NavigationStatusResponse(
            "running",
            100.0,
            120.0,
            420.0,
            210.0,
            "elder_room",
            "\u6b63\u5728\u524d\u5f80\u8001\u4eba\u623f\u95f4");

    private final RobotNavigationTaskRepository taskRepository;
    private final RobotStatusRepository statusRepository;
    private final RobotMapMarkerRepository markerRepository;

    public NavigationTaskService(
            RobotNavigationTaskRepository taskRepository,
            RobotStatusRepository statusRepository,
            RobotMapMarkerRepository markerRepository) {
        this.taskRepository = taskRepository;
        this.statusRepository = statusRepository;
        this.markerRepository = markerRepository;
    }

    @Transactional
    public NavigationTaskResponse createTask(NavigationTaskRequest request) {
        LocalDateTime now = LocalDateTime.now();
        RobotNavigationTask task = new RobotNavigationTask();
        task.setRobotId(request.robotId());
        task.setCreatorId(request.creatorId());
        task.setMapId(mapId(request.mapId()));
        task.setTargetName(request.targetName());
        task.setTargetX(request.targetX());
        task.setTargetY(request.targetY());
        task.setStatus("running");
        task.setStartedAt(now);
        RobotNavigationTask savedTask = taskRepository.save(task);

        upsertTargetMarker(request, now);
        updateRobotStatus(request.robotId(), "running", "\u6b63\u5728\u524d\u5f80" + request.targetName());

        return toTaskResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public NavigationStatusResponse getStatus() {
        Optional<RobotStatus> statusOptional = statusRepository.findFirstByOrderByUpdatedAtDesc();
        if (statusOptional.isEmpty()) {
            return DEFAULT_STATUS;
        }

        RobotStatus status = statusOptional.get();
        Optional<RobotNavigationTask> taskOptional = status.getRobotId() == null
                ? Optional.empty()
                : taskRepository.findLatestActiveTask(status.getRobotId());
        RobotNavigationTask task = taskOptional.orElse(null);
        return new NavigationStatusResponse(
                firstPresent(status.getNavStatus(), "idle"),
                status.getCurrentX(),
                status.getCurrentY(),
                task == null ? null : task.getTargetX(),
                task == null ? null : task.getTargetY(),
                task == null ? null : task.getTargetName(),
                firstPresent(status.getLastMessage(), "navigation status ready"));
    }

    @Transactional
    public Optional<NavigationTaskResponse> cancelTask(long id, long userId) {
        return taskRepository.findById(id)
                .map(task -> {
                    if (task.getCreatorId() == null || task.getCreatorId().longValue() != userId) {
                        throw new ApiException(4030, "forbidden");
                    }
                    task.setStatus("cancelled");
                    task.setFinishedAt(LocalDateTime.now());
                    RobotNavigationTask savedTask = taskRepository.save(task);
                    updateRobotStatus(task.getRobotId(), "paused", "navigation paused");
                    return toTaskResponse(savedTask);
                });
    }

    private void upsertTargetMarker(NavigationTaskRequest request, LocalDateTime now) {
        Long mapId = mapId(request.mapId());
        RobotMapMarker marker = request.robotId() == null
                ? new RobotMapMarker()
                : markerRepository.findActiveTargetMarker(mapId, request.robotId()).orElseGet(RobotMapMarker::new);
        marker.setMapId(mapId);
        marker.setRobotId(request.robotId());
        marker.setMarkerType("target");
        marker.setLevel("info");
        marker.setTitle(request.targetName());
        marker.setDescription("Navigation target: " + request.targetName());
        marker.setLocationName(request.targetName());
        marker.setLocationX(request.targetX());
        marker.setLocationY(request.targetY());
        marker.setSource("staff");
        marker.setStatus("active");
        marker.setCreatedBy(request.creatorId());
        marker.setNavigationStatus("running");
        marker.setEventTime(now);
        markerRepository.save(marker);
    }

    private void updateRobotStatus(Long robotId, String navStatus, String message) {
        if (robotId == null) {
            return;
        }
        RobotStatus status = statusRepository.findByRobotId(robotId).orElseGet(() -> {
            RobotStatus created = new RobotStatus();
            created.setRobotId(robotId);
            return created;
        });
        status.setNavStatus(navStatus);
        if (status.getCurrentX() == null) {
            status.setCurrentX(DEFAULT_STATUS.currentX());
        }
        if (status.getCurrentY() == null) {
            status.setCurrentY(DEFAULT_STATUS.currentY());
        }
        status.setLastMessage(message);
        statusRepository.save(status);
    }

    private static NavigationTaskResponse toTaskResponse(RobotNavigationTask task) {
        return new NavigationTaskResponse(
                task.getId(),
                task.getStatus(),
                task.getTargetX(),
                task.getTargetY(),
                task.getTargetName());
    }

    private static Long mapId(Long mapId) {
        return mapId == null ? DEFAULT_MAP_ID : mapId;
    }

    private static String firstPresent(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
