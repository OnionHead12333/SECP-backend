package com.smartelderly.api.exercise;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.exercise.dto.ElderExerciseCompleteRequest;
import com.smartelderly.api.exercise.dto.ElderExerciseStartRequest;
import com.smartelderly.api.exercise.dto.ExerciseReminderProgressResponse;
import com.smartelderly.security.ElderOwnedProfileResolver;
import com.smartelderly.service.exercise.ElderExerciseReminderService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 老人端锻炼提醒：今日进度、开始、完成
 */
@RestController
@RequestMapping("/v1/elder/exercise-reminders")
@Validated
public class ElderExerciseReminderController {

    private final ElderExerciseReminderService elderExerciseReminderService;
    private final ElderOwnedProfileResolver elderOwnedProfileResolver;

    public ElderExerciseReminderController(
            ElderExerciseReminderService elderExerciseReminderService,
            ElderOwnedProfileResolver elderOwnedProfileResolver) {
        this.elderExerciseReminderService = elderExerciseReminderService;
        this.elderOwnedProfileResolver = elderOwnedProfileResolver;
    }

    @GetMapping("/today-progress")
    public ApiResponse<ExerciseReminderProgressResponse> getTodayProgress(
            @RequestParam("elderId") Long elderId) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(elderId);
        return ApiResponse.ok(elderExerciseReminderService.getTodayProgress(profileId));
    }

    @PostMapping("/{reminderId}/start")
    public ApiResponse<Void> startExercise(
            @PathVariable Long reminderId,
            @Valid @RequestBody ElderExerciseStartRequest request) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(request.getElderId());
        request.setElderId(profileId);
        elderExerciseReminderService.startExercise(reminderId, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{reminderId}/complete")
    public ApiResponse<ExerciseReminderProgressResponse> completeExercise(
            @PathVariable Long reminderId,
            @Valid @RequestBody ElderExerciseCompleteRequest request) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(request.getElderId());
        request.setElderId(profileId);
        return ApiResponse.ok(elderExerciseReminderService.completeExercise(reminderId, request));
    }
}
