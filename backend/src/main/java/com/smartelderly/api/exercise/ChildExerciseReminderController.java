package com.smartelderly.api.exercise;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.exercise.dto.ExerciseReminderCreateRequest;
import com.smartelderly.api.exercise.dto.ExerciseReminderResponse;
import com.smartelderly.api.exercise.dto.ExerciseReminderUpdateRequest;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.UserRole;
import com.smartelderly.domain.exercise.ExerciseReminderRepository;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.exercise.ExerciseReminderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 子女端锻炼提醒控制器
 * 路径：/v1/child/exercise-reminders
 */
@RestController
@RequestMapping("/v1/child/exercise-reminders")
@Validated
public class ChildExerciseReminderController {

    private final ExerciseReminderService exerciseReminderService;
    private final FamilyBindingRepository familyBindingRepository;
    private final ExerciseReminderRepository exerciseReminderRepository;

    public ChildExerciseReminderController(ExerciseReminderService exerciseReminderService,
                                           FamilyBindingRepository familyBindingRepository,
                                           ExerciseReminderRepository exerciseReminderRepository) {
        this.exerciseReminderService = exerciseReminderService;
        this.familyBindingRepository = familyBindingRepository;
        this.exerciseReminderRepository = exerciseReminderRepository;
    }

    /**
     * 校验当前子女与老人有 active 绑定关系
     */
    private void checkChildBoundToElder(Long elderProfileId) {
        var user = SecurityUtils.requireRole(UserRole.child);
        if (!familyBindingRepository.existsByChildUserIdAndElderProfileIdAndStatus(
                user.userId(), elderProfileId, BindingStatus.active)) {
            throw new ApiException(403, "无权操作该老人档案");
        }
    }

    /**
     * 从提醒 ID 获取老人 ID，并校验绑定
     */
    private void checkChildBoundToReminder(Long reminderId) {
        var reminder = exerciseReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(404, "锻炼提醒不存在"));
        checkChildBoundToElder(reminder.getElderProfileId());
    }

    /**
     * 1. 列表查询：按老人档案 ID 获取锻炼提醒
     * GET /v1/child/exercise-reminders?elderProfileId=1
     * 
     * 前端约定：
     * - data 必须是数组（即使为空）
     * - code == 0 表示成功
     */
    @GetMapping
    public ApiResponse<List<ExerciseReminderResponse>> listExerciseReminders(
            @RequestParam("elderProfileId") Long elderProfileId) {
        checkChildBoundToElder(elderProfileId);
        return ApiResponse.ok(exerciseReminderService.listByElderProfileId(elderProfileId));
    }

    /**
     * 2. 新建锻炼提醒
     * POST /v1/child/exercise-reminders
     * 
     * 前端约定：
     * - 若 title 为空，前端会先生成默认标题
     * - sourceType、status、createdBy 由服务端固定
     * - 响应体需返回新建记录或至少包含 id
     */
    @PostMapping
    public ApiResponse<ExerciseReminderResponse> createExerciseReminder(
            @Valid @RequestBody ExerciseReminderCreateRequest request) {
        checkChildBoundToElder(request.getElderProfileId());
        ExerciseReminderResponse response = exerciseReminderService.create(request);
        return ApiResponse.ok(response);
    }

    /**
     * 3. 修改锻炼提醒
     * PUT /v1/child/exercise-reminders/{id}
     * 
     * 前端约定：
     * - 响应体可为空（HTTP 2xx 无响应体时前端不校验 ApiResponse）
     * - 或返回完整的更新后记录且 code == 0
     */
    @PutMapping("/{id}")
    public ApiResponse<ExerciseReminderResponse> updateExerciseReminder(
            @PathVariable Long id,
            @Valid @RequestBody ExerciseReminderUpdateRequest request) {
        checkChildBoundToReminder(id);
        ExerciseReminderResponse response = exerciseReminderService.update(id, request);
        return ApiResponse.ok(response);
    }

    /**
     * 4. 删除锻炼提醒
     * DELETE /v1/child/exercise-reminders/{id}
     * 
     * 前端约定：
     * - HTTP 2xx 表示成功，不解析响应体
     * - 删除后前端会重新请求列表刷新
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExerciseReminder(@PathVariable Long id) {
        checkChildBoundToReminder(id);
        exerciseReminderService.delete(id);
    }
}
