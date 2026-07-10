package com.smartelderly.service.exercise;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.exercise.dto.ElderExerciseCompleteRequest;
import com.smartelderly.api.exercise.dto.ElderExerciseStartRequest;
import com.smartelderly.api.exercise.dto.ExerciseReminderProgressResponse;
import com.smartelderly.domain.ReminderExecutionLog;
import com.smartelderly.domain.ReminderExecutionLogRepository;
import com.smartelderly.domain.exercise.ExerciseReminder;
import com.smartelderly.domain.exercise.ExerciseReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 老人端锻炼提醒：今日进度与完成记录（写入 reminder_execution_logs，kind=exercise）
 */
@Service
public class ElderExerciseReminderService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String KIND = "exercise";

    private final ExerciseReminderRepository exerciseReminderRepository;
    private final ReminderExecutionLogRepository executionLogRepository;

    public ElderExerciseReminderService(
            ExerciseReminderRepository exerciseReminderRepository,
            ReminderExecutionLogRepository executionLogRepository) {
        this.exerciseReminderRepository = exerciseReminderRepository;
        this.executionLogRepository = executionLogRepository;
    }

    public ExerciseReminderProgressResponse getTodayProgress(Long elderProfileId) {
        LocalDateTime dayStart = LocalDate.now(ZONE).atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<ExerciseReminder> plans = exerciseReminderRepository
                .findByElderProfileIdAndEnabledTrueOrderByIdAsc(elderProfileId);
        int plannedCount = Math.max(plans.size(), 0);

        List<ReminderExecutionLog> todayLogs = executionLogRepository
                .findByElderProfileIdAndReminderKindAndScheduledAtBetween(elderProfileId, KIND, dayStart, dayEnd);

        List<ReminderExecutionLog> confirmedToday = todayLogs.stream()
                .filter(l -> "confirmed".equals(l.getStatus()))
                .toList();

        Set<Long> allConfirmedIds = confirmedToday.stream()
                .map(ReminderExecutionLog::getReminderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> missedReminderIds = todayLogs.stream()
                .filter(l -> "missed".equals(l.getStatus()))
                .map(ReminderExecutionLog::getReminderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int completedCount = (int) plans.stream()
                .filter(p -> allConfirmedIds.contains(p.getId()))
                .count();

        int missedCount = (int) plans.stream()
                .filter(p -> missedReminderIds.contains(p.getId()))
                .count();

        int pendingCount = (int) plans.stream()
                .filter(p -> !allConfirmedIds.contains(p.getId()))
                .filter(p -> !missedReminderIds.contains(p.getId()))
                .count();

        Long activeReminderId = plans.stream()
                .map(ExerciseReminder::getId)
                .filter(id -> !allConfirmedIds.contains(id))
                .findFirst()
                .orElse(0L);

        String nextReminderAt = null;
        if (activeReminderId != null && activeReminderId > 0 && pendingCount > 0) {
            ExerciseReminder active = plans.stream()
                    .filter(p -> Objects.equals(p.getId(), activeReminderId))
                    .findFirst()
                    .orElse(null);
            if (active != null) {
                LocalDate today = LocalDate.now(ZONE);
                LocalDateTime rt = active.getRemindTime();
                LocalDateTime nominal = rt.toLocalDate().equals(today)
                        ? rt
                        : LocalDateTime.of(today, rt.toLocalTime());
                nextReminderAt = nominal.atZone(ZONE).withZoneSameInstant(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_INSTANT);
            }
        }

        ReminderExecutionLog lastDone = confirmedToday.stream()
                .filter(l -> l.getConfirmedAt() != null)
                .max(Comparator.comparing(ReminderExecutionLog::getConfirmedAt))
                .orElse(null);

        String lastCompletedAt = null;
        String lastStatus = "pending";
        String lastSource = "manual";
        if (lastDone != null) {
            lastCompletedAt = lastDone.getConfirmedAt().atZone(ZONE).withZoneSameInstant(java.time.ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);
            lastSource = lastDone.getConfirmSource() != null ? lastDone.getConfirmSource() : "manual";
            lastStatus = "sensor".equals(lastSource) ? "sensor_verified" : "self_confirmed";
        }

        return ExerciseReminderProgressResponse.builder()
                .plannedCount(plannedCount)
                .completedCount(completedCount)
                .missedCount(missedCount)
                .pendingCount(pendingCount)
                .lastCompletionStatus(lastStatus)
                .lastCompletionSource(lastSource)
                .activeReminderId(activeReminderId == null ? 0L : activeReminderId)
                .nextReminderAt(nextReminderAt)
                .lastCompletedAt(lastCompletedAt)
                .build();
    }

    @Transactional
    public void startExercise(Long reminderId, ElderExerciseStartRequest req) {
        ExerciseReminder r = exerciseReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(404, "锻炼提醒不存在"));
        if (!Objects.equals(r.getElderProfileId(), req.getElderId())) {
            throw new ApiException(403, "无权操作该锻炼提醒");
        }
        parseIso(req.getStartedAt());
    }

    @Transactional
    public ExerciseReminderProgressResponse completeExercise(Long reminderId, ElderExerciseCompleteRequest req) {
        ExerciseReminder r = exerciseReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(404, "锻炼提醒不存在"));
        if (!Objects.equals(r.getElderProfileId(), req.getElderId())) {
            throw new ApiException(403, "无权操作该锻炼提醒");
        }

        String src = req.getSource();
        if (src == null || src.isBlank()) {
            src = "manual";
        }
        if (!"sensor".equals(src)) {
            src = "manual";
        }

        LocalDateTime confirmedAt = parseIso(req.getConfirmedAt());

        ReminderExecutionLog log = new ReminderExecutionLog();
        log.setElderProfileId(req.getElderId());
        log.setReminderId(reminderId);
        log.setReminderKind(KIND);
        log.setScheduledAt(confirmedAt);
        log.setStatus("confirmed");
        log.setConfirmedAt(confirmedAt);
        log.setConfirmSource(src);
        log.setIsTimeout(false);
        executionLogRepository.save(log);

        return getTodayProgress(req.getElderId());
    }

    private static LocalDateTime parseIso(String iso) {
        try {
            return LocalDateTime.ofInstant(Instant.parse(iso), ZONE);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                throw new ApiException(400, "时间格式无效");
            }
        }
    }
}
