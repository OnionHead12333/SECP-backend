package com.smartelderly.service;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.WaterReminderCreateRequest;
import com.smartelderly.api.dto.WaterReminderProgressResponse;
import com.smartelderly.api.dto.WaterReminderResponse;
import com.smartelderly.api.dto.WaterReminderUpdateRequest;
import com.smartelderly.domain.ReminderExecutionLog;
import com.smartelderly.domain.ReminderExecutionLogRepository;
import com.smartelderly.domain.WaterReminder;
import com.smartelderly.domain.WaterReminderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WaterReminderService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MISSED_TIMEOUT_MINUTES = 10; // pending 超过计划时间 N 分钟后判为 missed

    private final WaterReminderRepository waterReminderRepository;
    private final ReminderExecutionLogRepository executionLogRepository;

    public WaterReminderService(WaterReminderRepository waterReminderRepository,
                                ReminderExecutionLogRepository executionLogRepository) {
        this.waterReminderRepository = waterReminderRepository;
        this.executionLogRepository = executionLogRepository;
    }

    // ==================== 子女端接口 ====================

    public List<WaterReminderResponse> listByElderProfileId(Long elderProfileId) {
        return waterReminderRepository.findByElderProfileIdOrderByCreatedAtDesc(elderProfileId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public WaterReminderResponse create(Long childUserId, WaterReminderCreateRequest request) {
        WaterReminder reminder = new WaterReminder();
        reminder.setElderProfileId(request.getElderProfileId());
        reminder.setTitle(request.getTitle());
        reminder.setDailyTargetMl(request.getDailyTargetMl());
        reminder.setIntervalMinutes(request.getIntervalMinutes());
        reminder.setSourceType(request.getSourceType());
        reminder.setStatus(request.getStatus());
        reminder.setCreatedBy(request.getCreatedBy());
        reminder.setEnabled(true);

        if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
            reminder.setStartTime(LocalTime.parse(request.getStartTime(), TIME_FMT));
        }
        if (request.getEndTime() != null && !request.getEndTime().isBlank()) {
            reminder.setEndTime(LocalTime.parse(request.getEndTime(), TIME_FMT));
        }
        if (request.getRemindTime() != null && !request.getRemindTime().isBlank()) {
            reminder.setRemindTime(parseIso8601(request.getRemindTime()));
        }

        reminder = waterReminderRepository.save(reminder);
        return toResponse(reminder);
    }

    @Transactional
    public WaterReminderResponse update(Long id, WaterReminderUpdateRequest request) {
        WaterReminder reminder = waterReminderRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "喝水提醒不存在"));
        reminder.setTitle(request.getTitle());
        reminder.setDailyTargetMl(request.getDailyTargetMl());
        reminder.setIntervalMinutes(request.getIntervalMinutes());

        if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
            reminder.setStartTime(LocalTime.parse(request.getStartTime(), TIME_FMT));
        }
        if (request.getEndTime() != null && !request.getEndTime().isBlank()) {
            reminder.setEndTime(LocalTime.parse(request.getEndTime(), TIME_FMT));
        }

        reminder = waterReminderRepository.save(reminder);
        deletePendingLogsForReminder(id);
        return toResponse(reminder);
    }

    @Transactional
    public void delete(Long id) {
        WaterReminder reminder = waterReminderRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "喝水提醒不存在"));
        deletePendingLogsForReminder(id);
        waterReminderRepository.delete(reminder);
    }

    // ==================== 老人端接口 ====================

    /**
     * 获取今日喝水进度 — 从计划动态计算当日所有时间点，预生成 pending 执行日志，
     * nextReminderAt 只返回未来时间
     */
    @Transactional
    public WaterReminderProgressResponse getTodayProgress(Long elderId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        TodayWaterState state = prepareTodayState(elderId, today, now);

        int confirmedCount = 0;
        int missedCount = 0;
        for (ReminderExecutionLog log : state.countableLogs) {
            if ("confirmed".equals(log.getStatus())) {
                confirmedCount++;
            } else if ("missed".equals(log.getStatus())) {
                missedCount++;
            }
        }

        int plannedCount = state.plannedCount;
        int pendingCount = Math.max(0, plannedCount - confirmedCount - missedCount);
        double completionPercent = plannedCount > 0
                ? Math.round((double) confirmedCount / plannedCount * 1000.0) / 10.0 : 0.0;

        String nextReminderAt = null;
        Long activeReminderId = 0L;
        if (state.nextPending != null) {
            activeReminderId = state.nextPending.getReminderId();
            nextReminderAt = state.nextPending.getScheduledAt().atZone(ZONE)
                    .withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        }

        String lastConfirmedAt = null;
        ReminderExecutionLog lastConfirmed = executionLogRepository
            .findTopByElderProfileIdAndReminderKindAndStatusOrderByConfirmedAtDesc(
                elderId, "water", "confirmed");
        if (lastConfirmed != null && lastConfirmed.getConfirmedAt() != null) {
            lastConfirmedAt = lastConfirmed.getConfirmedAt().atZone(ZONE)
                .withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        }

        return WaterReminderProgressResponse.builder()
                .plannedCount(plannedCount)
                .confirmedCount(confirmedCount)
                .missedCount(missedCount)
                .pendingCount(pendingCount)
                .completionPercent(completionPercent)
                .activeReminderId(activeReminderId)
                .lastConfirmedAt(lastConfirmedAt)
                .nextReminderAt(nextReminderAt)
                .build();
    }

    /**
     * 确认喝水 — 找到最近一个 pending 执行日志标记为 confirmed
     */
    @Transactional
    public WaterReminderProgressResponse confirmWater(Long reminderId, Long elderId, String confirmedAtStr) {
        LocalDateTime confirmedAt = parseIso8601(confirmedAtStr);
        LocalDate today = LocalDate.now(ZONE);
        TodayWaterState state = prepareTodayState(elderId, today, LocalDateTime.now(ZONE));

        ReminderExecutionLog target = state.eligibleLogs.stream()
                .filter(l -> l.getReminderId().equals(reminderId))
                .filter(l -> "pending".equals(l.getStatus()))
                .filter(l -> !l.getScheduledAt().isAfter(confirmedAt))
                .max(Comparator.comparing(ReminderExecutionLog::getScheduledAt))
                .orElseGet(() -> state.eligibleLogs.stream()
                        .filter(l -> l.getReminderId().equals(reminderId))
                        .filter(l -> "pending".equals(l.getStatus()))
                        .filter(l -> l.getScheduledAt().isAfter(confirmedAt))
                        .min(Comparator.comparing(ReminderExecutionLog::getScheduledAt))
                        .orElse(null));

        if (target == null) {
            throw new ApiException(404, "没有待确认的喝水提醒");
        }

        target.setStatus("confirmed");
        target.setConfirmSource("manual");
        target.setConfirmedAt(confirmedAt);
        target.setIsTimeout(false);
        executionLogRepository.save(target);

        // 更新 WaterReminder 今日数据
        waterReminderRepository.findById(reminderId).ifPresent(wr -> {
            int perIntake = wr.getPerIntakeMl() != null ? wr.getPerIntakeMl() : 200;
            wr.setTodayIntakeMl((wr.getTodayIntakeMl() != null ? wr.getTodayIntakeMl() : 0) + perIntake);
            wr.setLastIntakeTime(confirmedAt);
            waterReminderRepository.save(wr);
        });

        return getTodayProgress(elderId);
    }

    /**
     * 稍后提醒 — 取消原 pending，新建一条顺延的 pending 日志
     */
    @Transactional
    public WaterReminderProgressResponse snoozeWater(Long reminderId, Long elderId,
                                                      int snoozeMinutes, String requestedAtStr) {
        if (snoozeMinutes != 1) {
            throw new ApiException(400, "稍后提醒当前固定为1分钟");
        }
        LocalDateTime requestedAt = parseIso8601(requestedAtStr);
        LocalDateTime snoozedAt = requestedAt.plusMinutes(snoozeMinutes);

        WaterReminder reminder = waterReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(404, "喝水提醒不存在"));

        // 校验 reminder 属于该老人
        if (!reminder.getElderProfileId().equals(elderId)) {
            throw new ApiException(403, "无权操作该喝水提醒");
        }

        TodayWaterState state = prepareTodayState(elderId, LocalDate.now(ZONE), LocalDateTime.now(ZONE));
        ReminderExecutionLog target = state.eligibleLogs.stream()
                .filter(l -> l.getReminderId().equals(reminderId))
                .filter(l -> "pending".equals(l.getStatus()))
                .filter(l -> !l.getScheduledAt().isAfter(requestedAt))
                .max(Comparator.comparing(ReminderExecutionLog::getScheduledAt))
                .orElseGet(() -> state.eligibleLogs.stream()
                        .filter(l -> l.getReminderId().equals(reminderId))
                        .filter(l -> "pending".equals(l.getStatus()))
                        .filter(l -> l.getScheduledAt().isAfter(requestedAt))
                        .min(Comparator.comparing(ReminderExecutionLog::getScheduledAt))
                        .orElse(null));
        if (target == null) {
            throw new ApiException(404, "没有可稍后提醒的喝水提醒");
        }
        target.setStatus("cancelled");
        executionLogRepository.save(target);

        // 2. 新建一条 pending 日志，时间顺延
        ReminderExecutionLog newLog = new ReminderExecutionLog();
        newLog.setElderProfileId(elderId);
        newLog.setReminderId(reminderId);
        newLog.setReminderKind("water");
        newLog.setScheduledAt(snoozedAt);
        newLog.setStatus("pending");
        newLog.setIsTimeout(false);
        executionLogRepository.save(newLog);

        // 3. 更新计划的 remind_time
        reminder.setRemindTime(snoozedAt);
        waterReminderRepository.save(reminder);

        return getTodayProgress(elderId);
    }

    // ==================== missed 超时判定 ====================

    /**
     * 定时任务：每分钟检查超时的 pending 执行日志，标记为 missed
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void markTimedOutAsMissed() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime dayStart = LocalDate.now(ZONE).atStartOfDay();
        LocalDateTime deadline = now.minusMinutes(MISSED_TIMEOUT_MINUTES);

        // 查询今日所有 pending 的喝水日志
        List<ReminderExecutionLog> pendingLogs = executionLogRepository
                .findPendingWaterLogsBefore(deadline, dayStart);

        for (ReminderExecutionLog log : pendingLogs) {
            log.setStatus("missed");
            log.setIsTimeout(true);
            executionLogRepository.save(log);
        }
    }

    // ==================== 私有工具方法 ====================

    private TodayWaterState prepareTodayState(Long elderId, LocalDate today, LocalDateTime now) {
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        LocalDateTime missedDeadline = now.minusMinutes(MISSED_TIMEOUT_MINUTES);

        List<WaterReminder> plans = waterReminderRepository.findEnabledByElderProfileId(elderId);
        Set<Long> activeReminderIds = plans.stream()
                .map(WaterReminder::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<ScheduleSlot> allSlots = buildTodaySlots(plans, today);

        List<ReminderExecutionLog> todayLogs = executionLogRepository
                .findTodayWaterLogs(elderId, dayStart, dayEnd);
        Map<String, ReminderExecutionLog> logIndex = new LinkedHashMap<>();
        for (ReminderExecutionLog log : todayLogs) {
            logIndex.put(logKey(log.getReminderId(), log.getScheduledAt()), log);
        }

        for (ScheduleSlot slot : allSlots) {
            String key = logKey(slot.reminderId, slot.scheduledAt);
            ReminderExecutionLog log = logIndex.get(key);
            if (log == null) {
                log = new ReminderExecutionLog();
                log.setElderProfileId(elderId);
                log.setReminderId(slot.reminderId);
                log.setReminderKind("water");
                log.setScheduledAt(slot.scheduledAt);
                log.setStatus("pending");
                log.setIsTimeout(false);
                executionLogRepository.save(log);
                logIndex.put(key, log);
            }
        }

        List<ReminderExecutionLog> refreshedLogs = executionLogRepository
                .findTodayWaterLogs(elderId, dayStart, dayEnd);
        List<ReminderExecutionLog> eligibleLogs = refreshedLogs.stream()
                .filter(log -> activeReminderIds.contains(log.getReminderId()))
                .collect(Collectors.toList());

        for (ReminderExecutionLog log : eligibleLogs) {
            if ("pending".equals(log.getStatus()) && !log.getScheduledAt().isAfter(missedDeadline)) {
                log.setStatus("missed");
                log.setIsTimeout(true);
                executionLogRepository.save(log);
            }
        }

        eligibleLogs = executionLogRepository.findTodayWaterLogs(elderId, dayStart, dayEnd).stream()
                .filter(log -> activeReminderIds.contains(log.getReminderId()))
                .collect(Collectors.toList());

        ReminderExecutionLog nextPending = eligibleLogs.stream()
                .filter(log -> "pending".equals(log.getStatus()))
                .filter(log -> log.getScheduledAt().isAfter(now))
                .min(Comparator.comparing(ReminderExecutionLog::getScheduledAt))
                .orElse(null);

        List<ReminderExecutionLog> countableLogs = eligibleLogs.stream()
                .filter(log -> !"cancelled".equals(log.getStatus()))
                .collect(Collectors.toList());

        return new TodayWaterState(allSlots.size(), eligibleLogs, countableLogs, nextPending);
    }

    private void deletePendingLogsForReminder(Long reminderId) {
        List<ReminderExecutionLog> pendingLogs = executionLogRepository
                .findByReminderIdAndStatus(reminderId, "pending");
        if (!pendingLogs.isEmpty()) {
            executionLogRepository.deleteAll(pendingLogs);
        }
    }

    private String logKey(Long reminderId, LocalDateTime scheduledAt) {
        return reminderId + ":" + scheduledAt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    }

    /**
     * 从提醒计划构建当日所有计划时间点
     */
    private List<ScheduleSlot> buildTodaySlots(List<WaterReminder> plans, LocalDate today) {
        List<ScheduleSlot> slots = new ArrayList<>();
        for (WaterReminder plan : plans) {
            if (plan.getStartTime() == null || plan.getEndTime() == null
                    || plan.getIntervalMinutes() == null || plan.getIntervalMinutes() <= 0) {
                continue;
            }
            int interval = plan.getIntervalMinutes();
            LocalTime start = plan.getStartTime();
            LocalTime end = plan.getEndTime();

            LocalTime cursor = start;
            while (!cursor.isAfter(end)) {
                LocalDateTime scheduledAt = LocalDateTime.of(today, cursor);
                slots.add(new ScheduleSlot(plan.getId(), scheduledAt));
                cursor = cursor.plusMinutes(interval);
            }
        }
        slots.sort(Comparator.comparing(ScheduleSlot::scheduledAt));
        return slots;
    }

    private static class ScheduleSlot {
        final Long reminderId;
        final LocalDateTime scheduledAt;

        ScheduleSlot(Long reminderId, LocalDateTime scheduledAt) {
            this.reminderId = reminderId;
            this.scheduledAt = scheduledAt;
        }

        LocalDateTime scheduledAt() {
            return scheduledAt;
        }
    }

    private static class TodayWaterState {
        final int plannedCount;
        final List<ReminderExecutionLog> eligibleLogs;
        final List<ReminderExecutionLog> countableLogs;
        final ReminderExecutionLog nextPending;

        TodayWaterState(int plannedCount,
                        List<ReminderExecutionLog> eligibleLogs,
                        List<ReminderExecutionLog> countableLogs,
                        ReminderExecutionLog nextPending) {
            this.plannedCount = plannedCount;
            this.eligibleLogs = eligibleLogs;
            this.countableLogs = countableLogs;
            this.nextPending = nextPending;
        }
    }

    private WaterReminderResponse toResponse(WaterReminder w) {
        return WaterReminderResponse.builder()
                .id(w.getId())
                .elderProfileId(w.getElderProfileId())
                .title(w.getTitle())
                .dailyTargetMl(w.getDailyTargetMl())
                .intervalMinutes(w.getIntervalMinutes())
                .startTime(w.getStartTime() != null ? w.getStartTime().format(TIME_FMT) : null)
                .endTime(w.getEndTime() != null ? w.getEndTime().format(TIME_FMT) : null)
                .todayIntakeMl(w.getTodayIntakeMl())
                .lastIntakeTime(w.getLastIntakeTime() != null
                        ? w.getLastIntakeTime().atZone(ZONE).format(DateTimeFormatter.ISO_INSTANT) : null)
                .sourceType(w.getSourceType())
                .remindTime(w.getRemindTime() != null
                        ? w.getRemindTime().atZone(ZONE).format(DateTimeFormatter.ISO_INSTANT) : null)
                .repeatRule(w.getRepeatRule())
                .enabled(w.getEnabled())
                .status(w.getStatus())
                .createdBy(w.getCreatedBy())
                .createdAt(w.getCreatedAt() != null ? w.getCreatedAt().atZone(ZONE).format(ISO_FMT) : null)
                .updatedAt(w.getUpdatedAt() != null ? w.getUpdatedAt().atZone(ZONE).format(ISO_FMT) : null)
                .build();
    }

    private LocalDateTime parseIso8601(String iso8601) {
        try {
            return LocalDateTime.ofInstant(Instant.parse(iso8601), ZONE);
        } catch (Exception e) {
            return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
