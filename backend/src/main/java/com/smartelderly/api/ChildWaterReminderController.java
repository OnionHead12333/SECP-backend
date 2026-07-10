package com.smartelderly.api;

import com.smartelderly.api.dto.WaterReminderCreateRequest;
import com.smartelderly.api.dto.WaterReminderResponse;
import com.smartelderly.api.dto.WaterReminderUpdateRequest;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.UserRole;
import com.smartelderly.domain.WaterReminderRepository;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.WaterReminderService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 子女端喝水提醒控制器
 */
@RestController
@RequestMapping("/v1/child")
@Validated
public class ChildWaterReminderController {

    private final WaterReminderService waterReminderService;
    private final FamilyBindingRepository familyBindingRepository;
    private final WaterReminderRepository waterReminderRepository;

    public ChildWaterReminderController(WaterReminderService waterReminderService,
                                         FamilyBindingRepository familyBindingRepository,
                                         WaterReminderRepository waterReminderRepository) {
        this.waterReminderService = waterReminderService;
        this.familyBindingRepository = familyBindingRepository;
        this.waterReminderRepository = waterReminderRepository;
    }

    // 校验当前子女与老人有 active 绑定关系
    private void checkChildBoundToElder(Long elderProfileId) {
        var user = SecurityUtils.requireRole(UserRole.child);
        if (!familyBindingRepository.existsByChildUserIdAndElderProfileIdAndStatus(
                user.userId(), elderProfileId, BindingStatus.active)) {
            throw new ApiException(403, "无权操作该老人档案");
        }
    }

    // 从提醒 ID 获取老人 ID，并校验绑定
    private void checkChildBoundToReminder(Long reminderId) {
        var reminder = waterReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(404, "喝水提醒不存在"));
        checkChildBoundToElder(reminder.getElderProfileId());
    }

    @GetMapping("/water-reminders")
    public ApiResponse<List<WaterReminderResponse>> listWaterReminders(
            @RequestParam("elderProfileId") Long elderProfileId) {
        checkChildBoundToElder(elderProfileId);
        return ApiResponse.ok(waterReminderService.listByElderProfileId(elderProfileId));
    }

    @PostMapping("/water-reminders")
    public ApiResponse<WaterReminderResponse> createWaterReminder(
            @Valid @RequestBody WaterReminderCreateRequest request) {
        checkChildBoundToElder(request.getElderProfileId());
        var user = SecurityUtils.requireRole(UserRole.child);
        return ApiResponse.ok(waterReminderService.create(user.userId(), request));
    }

    @PutMapping("/water-reminders/{id}")
    public ApiResponse<WaterReminderResponse> updateWaterReminder(
            @PathVariable("id") Long id,
            @Valid @RequestBody WaterReminderUpdateRequest request) {
        checkChildBoundToReminder(id);
        return ApiResponse.ok(waterReminderService.update(id, request));
    }

    @DeleteMapping("/water-reminders/{id}")
    public ApiResponse<Void> deleteWaterReminder(@PathVariable("id") Long id) {
        checkChildBoundToReminder(id);
        waterReminderService.delete(id);
        return ApiResponse.ok(null);
    }
}
