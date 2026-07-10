package com.smartelderly.service.impl;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.MedicineReminderConfirmDTO;
import com.smartelderly.api.dto.MedicineReminderProgressDTO;
import com.smartelderly.api.dto.MedicineReminderSnoozeDTO;
import com.smartelderly.domain.ReminderExecutionLog;
import com.smartelderly.domain.ReminderExecutionLogRepository;
import com.smartelderly.domain.medical.ChildMedicineReminder;
import com.smartelderly.domain.medical.ChildMedicineReminderRepository;
import com.smartelderly.service.ElderMedicineReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ElderMedicineReminderServiceImpl implements ElderMedicineReminderService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset UTC = ZoneOffset.UTC;
    private static final String KIND = "medicine";

    @Autowired
    private ChildMedicineReminderRepository childMedicineReminderRepository;

    @Autowired
    private ReminderExecutionLogRepository reminderExecutionLogRepository;

    private LocalDate remindCalendarDay(ChildMedicineReminder r) {
        return r.getRemindTime().toLocalDate();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private boolean isDaily(ChildMedicineReminder r) {
        return "daily".equalsIgnoreCase(nullToEmpty(r.getRepeatRule()))
                || "daily".equalsIgnoreCase(nullToEmpty(r.getFrequencyRule()));
    }

    private boolean isWeekly(ChildMedicineReminder r) {
        return "weekly".equalsIgnoreCase(nullToEmpty(r.getRepeatRule()))
                || "weekly".equalsIgnoreCase(nullToEmpty(r.getFrequencyRule()));
    }

    /**
     * 上海「今天」下该条提醒是否计入今日进度；若是则返回今日应提醒的墙上时间（用于统计、确认落库）。
     * <p>
     * 子女端常把「每天服用」写在 frequencyRule，而 repeatRule 仍为 none；若只按 remind_time 日历日过滤会导致新建后老人端一直 0 次。
     */
    private LocalDateTime todaysOccurrence(ChildMedicineReminder r, LocalDate today) {
        if (r.getRemindTime() == null) {
            return null;
        }
        LocalTime clock = r.getRemindTime().toLocalTime();
        if (isDaily(r)) {
            return LocalDateTime.of(today, clock);
        }
        if (isWeekly(r)) {
            if (!r.getRemindTime().getDayOfWeek().equals(today.getDayOfWeek())) {
                return null;
            }
            return LocalDateTime.of(today, clock);
        }
        if (!remindCalendarDay(r).equals(today)) {
            return null;
        }
        return r.getRemindTime();
    }

    @Override
    public MedicineReminderProgressDTO getTodayProgress(Long elderId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<ChildMedicineReminder> todayPlans = childMedicineReminderRepository
                .findByElderProfileIdOrderByRemindTimeAsc(elderId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .filter(r -> todaysOccurrence(r, today) != null)
                .sorted(Comparator.comparing(r -> todaysOccurrence(r, today)))
                .toList();

        int plannedCount = todayPlans.size();

        List<ReminderExecutionLog> logs = reminderExecutionLogRepository
                .findByElderProfileIdAndReminderKindAndScheduledAtBetween(elderId, KIND, dayStart, dayEnd);

        Set<Long> confirmedIds = logs.stream()
                .filter(l -> "confirmed".equals(l.getStatus()))
                .map(ReminderExecutionLog::getReminderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> missedIds = logs.stream()
                .filter(l -> "missed".equals(l.getStatus()))
                .map(ReminderExecutionLog::getReminderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 仅统计「今日计划 todayPlans」内的确认/漏服，避免其它 reminderId 的日志把 pending 错算成 0
        int confirmedCount = (int) todayPlans.stream()
                .filter(r -> confirmedIds.contains(r.getId()))
                .count();
        int missedDistinct = (int) todayPlans.stream()
                .filter(r -> missedIds.contains(r.getId()))
                .count();
        int pendingCount = (int) todayPlans.stream()
                .filter(r -> !confirmedIds.contains(r.getId()))
                .filter(r -> !missedIds.contains(r.getId()))
                .count();

        int activeReminderId = 0;
        String medicineName = null;
        String doseDesc = null;
        String nextReminderAt = null;

        for (ChildMedicineReminder r : todayPlans) {
            if (!confirmedIds.contains(r.getId())) {
                activeReminderId = r.getId().intValue();
                medicineName = r.getMedicineName();
                doseDesc = r.getDosage();
                LocalDateTime occ = todaysOccurrence(r, today);
                nextReminderAt = occ != null
                        ? occ.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : r.getRemindTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                break;
            }
        }

        ReminderExecutionLog last = logs.stream()
                .filter(l -> "confirmed".equals(l.getStatus()) && l.getConfirmedAt() != null)
                .max(Comparator.comparing(ReminderExecutionLog::getConfirmedAt))
                .orElse(null);
        String lastConfirmedAt = last != null
                ? last.getConfirmedAt().atZone(ZONE).withZoneSameInstant(UTC).format(DateTimeFormatter.ISO_INSTANT)
                : null;

        double completionPercent = plannedCount == 0 ? 0 : Math.round(confirmedCount * 1000.0 / plannedCount) / 10.0;

        MedicineReminderProgressDTO dto = new MedicineReminderProgressDTO();
        dto.plannedCount = plannedCount;
        dto.confirmedCount = confirmedCount;
        dto.missedCount = missedDistinct;
        dto.pendingCount = pendingCount;
        dto.completionPercent = completionPercent;
        dto.activeReminderId = activeReminderId;
        dto.medicineName = medicineName != null ? medicineName : "-";
        dto.doseDesc = doseDesc;
        dto.lastConfirmedAt = lastConfirmedAt;
        dto.nextReminderAt = nextReminderAt;
        return dto;
    }

    @Override
    @Transactional
    public MedicineReminderProgressDTO confirmMedicine(Long reminderId, MedicineReminderConfirmDTO confirmDTO) {
        ChildMedicineReminder r = childMedicineReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(404, "吃药提醒不存在"));
        if (!r.getElderProfileId().equals(Long.valueOf(confirmDTO.elderId))) {
            throw new ApiException(403, "无权操作该吃药提醒");
        }
        LocalDateTime confirmedAt = parseIso(confirmDTO.confirmedAt);
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime slot = todaysOccurrence(r, today);
        if (slot == null) {
            throw new ApiException(400, "今日无该吃药提醒计划");
        }

        ReminderExecutionLog log = new ReminderExecutionLog();
        log.setElderProfileId(r.getElderProfileId());
        log.setReminderId(r.getId());
        log.setReminderKind(KIND);
        log.setScheduledAt(slot);
        log.setStatus("confirmed");
        log.setConfirmedAt(confirmedAt);
        log.setConfirmSource("manual");
        log.setIsTimeout(false);
        reminderExecutionLogRepository.save(log);

        return getTodayProgress(Long.valueOf(confirmDTO.elderId));
    }

    @Override
    public MedicineReminderProgressDTO snoozeReminder(Long reminderId, MedicineReminderSnoozeDTO snoozeDTO) {
        return getTodayProgress(Long.valueOf(snoozeDTO.elderId));
    }

    private LocalDateTime parseIso(String iso) {
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
