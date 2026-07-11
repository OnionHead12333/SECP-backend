package com.smartelderly.api.entertainment;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.InspectionApiResponse;

@RestController
@RequestMapping({"/entertainment", "/api/entertainment"})
public class EntertainmentController {

    private final EntertainmentService entertainmentService;

    public EntertainmentController(EntertainmentService entertainmentService) {
        this.entertainmentService = entertainmentService;
    }

    @GetMapping("/music")
    public InspectionApiResponse<List<MusicResponse>> listMusic() {
        return InspectionApiResponse.ok(entertainmentService.listMusic());
    }

    @PostMapping("/music/play")
    public InspectionApiResponse<EntertainmentTaskResponse> playMusic(
            @RequestBody EntertainmentCommandRequest request) {
        return InspectionApiResponse.ok(entertainmentService.playMusic(request));
    }

    @PostMapping("/dance/start")
    public InspectionApiResponse<EntertainmentTaskResponse> startDance(
            @RequestBody EntertainmentCommandRequest request) {
        return InspectionApiResponse.ok(entertainmentService.startDance(request));
    }

    @GetMapping("/tasks")
    public InspectionApiResponse<List<EntertainmentTaskResponse>> listTasks() {
        return InspectionApiResponse.ok(entertainmentService.listRecentTasks());
    }

    @GetMapping("/tasks/pending")
    public InspectionApiResponse<List<PendingEntertainmentTaskResponse>> listPendingTasks() {
        return InspectionApiResponse.ok(entertainmentService.listPendingTasks());
    }

    @PutMapping("/tasks/{taskId}/status")
    public InspectionApiResponse<UpdateEntertainmentTaskStatusResponse> updateTaskStatus(
            @PathVariable long taskId,
            @RequestBody UpdateEntertainmentTaskStatusRequest request) {
        return entertainmentService.updateTaskStatus(taskId, request)
                .map(response -> new InspectionApiResponse<>(true, "任务状态已更新", response))
                .orElseGet(() -> InspectionApiResponse.fail("task not found"));
    }

    @GetMapping("/status")
    public InspectionApiResponse<EntertainmentStatusResponse> getStatus() {
        return InspectionApiResponse.ok(entertainmentService.getLatestStatus());
    }
}
