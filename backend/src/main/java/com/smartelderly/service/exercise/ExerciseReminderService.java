package com.smartelderly.service.exercise;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.exercise.dto.ExerciseReminderCreateRequest;
import com.smartelderly.api.exercise.dto.ExerciseReminderResponse;
import com.smartelderly.api.exercise.dto.ExerciseReminderUpdateRequest;
import com.smartelderly.domain.exercise.ExerciseReminder;
import com.smartelderly.domain.exercise.ExerciseReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 锻炼提醒服务
 */
@Service
public class ExerciseReminderService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ExerciseReminderRepository exerciseReminderRepository;

    public ExerciseReminderService(ExerciseReminderRepository exerciseReminderRepository) {
        this.exerciseReminderRepository = exerciseReminderRepository;
    }

    /**
     * 列表查询：按老人档案 ID 获取锻炼提醒，返回数组
     * 前端会将响应体 data 视为数组处理
     */
    public List<ExerciseReminderResponse> listByElderProfileId(Long elderProfileId) {
        return exerciseReminderRepository.findByElderProfileIdOrderByCreatedAtDesc(elderProfileId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 创建锻炼提醒
     */
    @Transactional
    public ExerciseReminderResponse create(ExerciseReminderCreateRequest request) {
        ExerciseReminder reminder = new ExerciseReminder();
        reminder.setElderProfileId(request.getElderProfileId());
        reminder.setTitle(request.getTitle());
        reminder.setExerciseType(request.getExerciseType());
        reminder.setRepeatRule(request.getRepeatRule());
        
        // 解析时间字符串
        if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
            reminder.setStartTime(LocalTime.parse(request.getStartTime(), TIME_FMT));
        }
        if (request.getEndTime() != null && !request.getEndTime().isBlank()) {
            reminder.setEndTime(LocalTime.parse(request.getEndTime(), TIME_FMT));
        }

        // 提醒时刻与子女端「开始时间」对齐（上海时区）；请求里的 remindTime 仅兼容旧客户端，此处不再采用。
        if (reminder.getStartTime() == null) {
            throw new ApiException(400, "开始时间无效");
        }
        reminder.setRemindTime(remindDateTimeFromStartTime(reminder.getStartTime()));
        
        // 固定值（符合接口文档约定）
        reminder.setSourceType("child_remote");
        reminder.setStatus("pending");
        reminder.setCreatedBy("child");
        reminder.setEnabled(true);
        
        reminder = exerciseReminderRepository.save(reminder);
        return toResponse(reminder);
    }

    /**
     * 更新锻炼提醒（只支持修改基本信息）
     */
    @Transactional
    public ExerciseReminderResponse update(Long id, ExerciseReminderUpdateRequest request) {
        ExerciseReminder reminder = exerciseReminderRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "锻炼提醒不存在"));
        
        reminder.setTitle(request.getTitle());
        reminder.setExerciseType(request.getExerciseType());
        reminder.setRepeatRule(request.getRepeatRule());

        // 解析时间字符串；开始时间有变化时与创建逻辑一致，按新的开始时刻重算 remind_time
        LocalTime previousStart = reminder.getStartTime();
        if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
            LocalTime newStart = LocalTime.parse(request.getStartTime(), TIME_FMT);
            reminder.setStartTime(newStart);
            if (!Objects.equals(previousStart, newStart)) {
                reminder.setRemindTime(remindDateTimeFromStartTime(newStart));
            }
        }
        if (request.getEndTime() != null && !request.getEndTime().isBlank()) {
            reminder.setEndTime(LocalTime.parse(request.getEndTime(), TIME_FMT));
        }

        reminder = exerciseReminderRepository.save(reminder);
        return toResponse(reminder);
    }

    /**
     * 删除锻炼提醒
     */
    @Transactional
    public void delete(Long id) {
        ExerciseReminder reminder = exerciseReminderRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "锻炼提醒不存在"));
        exerciseReminderRepository.delete(reminder);
    }

    /**
     * 转换为响应体
     */
    private ExerciseReminderResponse toResponse(ExerciseReminder reminder) {
        ExerciseReminderResponse response = new ExerciseReminderResponse();
        response.setId(reminder.getId());
        response.setElderProfileId(reminder.getElderProfileId());
        response.setTitle(reminder.getTitle());
        response.setExerciseType(reminder.getExerciseType());
        response.setRepeatRule(reminder.getRepeatRule());
        response.setStatus(reminder.getStatus());
        
        // 格式化时间为 HH:mm:ss
        if (reminder.getStartTime() != null) {
            response.setStartTime(reminder.getStartTime().format(TIME_FMT));
        }
        if (reminder.getEndTime() != null) {
            response.setEndTime(reminder.getEndTime().format(TIME_FMT));
        }
        
        // 可选字段
        response.setGoalValue(reminder.getGoalValue());
        response.setGoalUnit(reminder.getGoalUnit());
        response.setIntervalMinutes(reminder.getIntervalMinutes());
        
        return response;
    }

    /**
     * 按开始时刻生成首次提醒用的 {@code remind_time}：取上海「当天」与该时刻；
     * 若该时刻已过，则顺延到次日同一时间（与每日重复场景下的下一次触发一致）。
     */
    private LocalDateTime remindDateTimeFromStartTime(LocalTime startTime) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime candidate = LocalDateTime.of(today, startTime);
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (!candidate.isBefore(now)) {
            return candidate;
        }
        return LocalDateTime.of(today.plusDays(1), startTime);
    }
}
