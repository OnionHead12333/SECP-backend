package com.smartelderly.api;

import com.smartelderly.api.dto.ConfirmWaterRequest;
import com.smartelderly.api.dto.SnoozeWaterRequest;
import com.smartelderly.api.dto.WaterReminderProgressResponse;
import com.smartelderly.domain.WaterReminderRepository;
import com.smartelderly.security.ElderOwnedProfileResolver;
import com.smartelderly.service.WaterReminderService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 老人端喝水提醒控制器
 */
@RestController
@RequestMapping("/v1/elder/water-reminders")
@Validated
public class ElderWaterReminderController {

    private final WaterReminderService waterReminderService;
    private final ElderOwnedProfileResolver elderOwnedProfileResolver;
    private final WaterReminderRepository waterReminderRepository;

    public ElderWaterReminderController(WaterReminderService waterReminderService,
                                         ElderOwnedProfileResolver elderOwnedProfileResolver,
                                         WaterReminderRepository waterReminderRepository) {
        this.waterReminderService = waterReminderService;
        this.elderOwnedProfileResolver = elderOwnedProfileResolver;
        this.waterReminderRepository = waterReminderRepository;
    }

    // 校验 reminderId 确实属于该老人
    private void checkReminderBelongsToElder(Long reminderId, Long elderId) {
        var reminder = waterReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(404, "喝水提醒不存在"));
        if (!Objects.equals(reminder.getElderProfileId(), elderId)) {
            throw new ApiException(403, "无权操作该喝水提醒");
        }
    }

    @GetMapping("/today-progress")
    public ApiResponse<WaterReminderProgressResponse> getTodayProgress(
            @RequestParam("elderId") Long elderId) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(elderId);
        return ApiResponse.ok(waterReminderService.getTodayProgress(profileId));
    }

    @PostMapping("/{reminderId}/confirm")
    public ApiResponse<WaterReminderProgressResponse> confirmWater(
            @PathVariable("reminderId") Long reminderId,
            @Valid @RequestBody ConfirmWaterRequest request) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(request.getElderId());
        request.setElderId(profileId);
        checkReminderBelongsToElder(reminderId, profileId);
        return ApiResponse.ok(waterReminderService.confirmWater(
                reminderId, profileId, request.getConfirmedAt()));
    }

    @PostMapping("/{reminderId}/snooze")
    public ApiResponse<WaterReminderProgressResponse> snoozeWater(
            @PathVariable("reminderId") Long reminderId,
            @Valid @RequestBody SnoozeWaterRequest request) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(request.getElderId());
        request.setElderId(profileId);
        checkReminderBelongsToElder(reminderId, profileId);
        return ApiResponse.ok(waterReminderService.snoozeWater(
                reminderId, profileId, request.getSnoozeMinutes(), request.getRequestedAt()));
    }
}
