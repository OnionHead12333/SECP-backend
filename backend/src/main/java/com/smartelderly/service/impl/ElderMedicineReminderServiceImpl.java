
package com.smartelderly.service.impl;

import com.smartelderly.domain.MedicineReminderRepository;
import com.smartelderly.domain.ReminderExecutionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.smartelderly.api.dto.MedicineReminderProgressDTO;
import com.smartelderly.api.dto.MedicineReminderConfirmDTO;
import com.smartelderly.api.dto.MedicineReminderSnoozeDTO;
import com.smartelderly.service.ElderMedicineReminderService;
import org.springframework.stereotype.Service;

@Service
public class ElderMedicineReminderServiceImpl implements ElderMedicineReminderService {

        @Autowired
        private MedicineReminderRepository medicineReminderRepository;

        @Autowired
        private ReminderExecutionLogRepository reminderExecutionLogRepository;
    @Override
    public MedicineReminderProgressDTO getTodayProgress(Integer elderId) {
        // 1. 获取当天时间范围
        java.time.LocalDateTime todayStart = java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime todayEnd = todayStart.plusDays(1);

        // 2. 查询今日所有启用的吃药提醒计划
        java.util.List<com.smartelderly.domain.MedicineReminder> reminders =
                medicineReminderRepository.findByElderIdAndRemindTimeBetweenAndEnabled(elderId, todayStart, todayEnd, true);

        int plannedCount = reminders.size();

        // 3. 查询今日所有执行日志
        java.util.List<com.smartelderly.domain.ReminderExecutionLog> logs =
                reminderExecutionLogRepository.findByElderIdAndScheduledAtBetween(elderId, todayStart, todayEnd);

        // 4. 统计 confirmed/missed/pending
        int confirmedCount = 0;
        int missedCount = 0;
        int pendingCount = 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // 5. 统计 lastConfirmedAt
        String lastConfirmedAt = null;
        java.time.LocalDateTime lastConfirmedTime = null;

        // 6. 遍历提醒计划，结合执行日志，判定状态
        int activeReminderId = 0;
        String medicineName = null;
        String doseDesc = null;
        String nextReminderAt = null;

        for (com.smartelderly.domain.MedicineReminder reminder : reminders) {
            // 找到该提醒的所有执行日志
            java.util.List<com.smartelderly.domain.ReminderExecutionLog> reminderLogs =
                    logs.stream().filter(l -> l.reminderId == reminder.id).toList();

            boolean confirmed = reminderLogs.stream().anyMatch(l -> "confirmed".equals(l.status));
            boolean snoozed = reminderLogs.stream().anyMatch(l -> "snoozed".equals(l.status));
            boolean missed = false;
            java.time.LocalDateTime scheduledAt = reminder.remindTime;
            java.time.LocalDateTime deadlineAt = scheduledAt.plusMinutes(2); // 假设2分钟为超时时间，可配置
            // snooze顺延
            if (snoozed) {
                // 找到最后一次snooze的时间和顺延分钟
                com.smartelderly.domain.ReminderExecutionLog lastSnooze = reminderLogs.stream()
                        .filter(l -> "snoozed".equals(l.status))
                        .max(java.util.Comparator.comparing(l -> l.scheduledAt)).orElse(null);
                if (lastSnooze != null) {
                    deadlineAt = lastSnooze.scheduledAt.plusMinutes(1); // 当前snooze固定1分钟
                }
            }
            if (!confirmed && now.isAfter(deadlineAt)) {
                missed = true;
            }

            if (confirmed) {
                confirmedCount++;
                // 记录最近一次确认时间
                com.smartelderly.domain.ReminderExecutionLog confirmLog = reminderLogs.stream()
                        .filter(l -> "confirmed".equals(l.status) && l.confirmedAt != null)
                        .max(java.util.Comparator.comparing(l -> l.confirmedAt)).orElse(null);
                if (confirmLog != null && (lastConfirmedTime == null || confirmLog.confirmedAt.isAfter(lastConfirmedTime))) {
                    lastConfirmedTime = confirmLog.confirmedAt;
                }
            } else if (missed) {
                missedCount++;
            } else {
                pendingCount++;
                // 只取第一个未完成的作为active
                if (activeReminderId == 0) {
                    activeReminderId = reminder.id;
                    medicineName = reminder.medicineName;
                    doseDesc = reminder.dosage;
                    nextReminderAt = reminder.remindTime.toString();
                }
            }
        }

        if (lastConfirmedTime != null) {
            lastConfirmedAt = lastConfirmedTime.toString();
        }

        double completionPercent = plannedCount == 0 ? 0 : ((double) confirmedCount / plannedCount) * 100.0;

        MedicineReminderProgressDTO dto = new MedicineReminderProgressDTO();
        dto.plannedCount = plannedCount;
        dto.confirmedCount = confirmedCount;
        dto.missedCount = missedCount;
        dto.pendingCount = pendingCount;
        dto.completionPercent = completionPercent;
        dto.activeReminderId = activeReminderId;
        dto.medicineName = medicineName;
        dto.doseDesc = doseDesc;
        dto.lastConfirmedAt = lastConfirmedAt;
        dto.nextReminderAt = nextReminderAt;
        return dto;
    }

    @Override
    public MedicineReminderProgressDTO confirmMedicine(Integer reminderId, MedicineReminderConfirmDTO confirmDTO) {
        // 1. 根据reminderId和elderId查找对应的提醒计划
        // MedicineReminder reminder = ...
        // 2. 新增或更新一条 reminder_execution_logs，status=confirmed，confirmedAt=confirmDTO.confirmedAt
        // 3. 可选：若有snooze，合并处理
        // 4. 重新聚合今日进度，调用getTodayProgress
        return getTodayProgress(confirmDTO.elderId);
    }

    @Override
    public MedicineReminderProgressDTO snoozeReminder(Integer reminderId, MedicineReminderSnoozeDTO snoozeDTO) {
        // 1. 根据reminderId和elderId查找对应的提醒计划
        // MedicineReminder reminder = ...
        // 2. 新增一条 reminder_execution_logs，status=snoozed，snoozeMinutes=snoozeDTO.snoozeMinutes，requestedAt=snoozeDTO.requestedAt
        // 3. 更新提醒计划的下次提醒时间（remindTime顺延snoozeMinutes）
        // 4. 重新聚合今日进度，调用getTodayProgress
        return getTodayProgress(snoozeDTO.elderId);
    }
}