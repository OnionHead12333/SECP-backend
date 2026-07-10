package com.smartelderly.service.medical;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.medical.dto.ChildMedicineReminderCreateRequest;
import com.smartelderly.api.medical.dto.ChildMedicineReminderUpdateRequest;
import com.smartelderly.api.medical.dto.MedicineReminderViewDto;
import com.smartelderly.domain.medical.ChildMedicineReminder;
import com.smartelderly.domain.medical.ChildMedicineReminderRepository;
import com.smartelderly.security.AuthPrincipal;

@Service
public class ChildMedicineReminderService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ISO_LOCAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MedicalAccessService medicalAccessService;
    private final ChildMedicineReminderRepository medicineReminderRepository;

    public ChildMedicineReminderService(
            MedicalAccessService medicalAccessService,
            ChildMedicineReminderRepository medicineReminderRepository) {
        this.medicalAccessService = medicalAccessService;
        this.medicineReminderRepository = medicineReminderRepository;
    }

    public List<MedicineReminderViewDto> listMedicineReminders(
            AuthPrincipal principal, Long elderProfileId) {
        long resolvedElderProfileId =
                medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        return medicineReminderRepository
                .findByElderProfileIdOrderByRemindTimeAsc(resolvedElderProfileId)
                .stream()
                .map(ChildMedicineReminderService::toViewDto)
                .toList();
    }
@Transactional
    public MedicineReminderViewDto createMedicineReminder(
            AuthPrincipal principal, ChildMedicineReminderCreateRequest request) {
        long resolvedElderProfileId =
                medicalAccessService.resolveTargetElderProfileId(principal, request.getElderProfileId());

        ChildMedicineReminder reminder = new ChildMedicineReminder();
        reminder.setElderProfileId(resolvedElderProfileId);
        reminder.setTitle(request.getTitle().trim());
        reminder.setMedicineName(request.getMedicineName().trim());
        reminder.setDosage(trimToNull(request.getDosage()));
        reminder.setFrequencyRule(defaultRule(trimToNull(request.getFrequencyRule())));
        reminder.setRepeatRule(defaultRule(trimToNull(request.getRepeatRule())));
        reminder.setRemindTime(instantToShanghaiWall(request.getRemindTime()));
        reminder.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        reminder.setRelatedEventId(request.getRelatedEventId());
        reminder.setSourceType("child_remote");
        reminder.setStatus("pending");
        reminder.setCreatedBy("child");
        reminder.setCreatedAt(LocalDateTime.now(DISPLAY_ZONE));
        reminder.setUpdatedAt(LocalDateTime.now(DISPLAY_ZONE));

        reminder = medicineReminderRepository.save(reminder);
        return toViewDto(reminder);
    }

    @Transactional
    public MedicineReminderViewDto updateMedicineReminder(
            AuthPrincipal principal, Long reminderId, ChildMedicineReminderUpdateRequest request) {
        // 查询提醒记录
        ChildMedicineReminder reminder = medicineReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(4004, "吃药提醒不存在"));

        // 验证权限：确保子女可以访问该老人的记录
        medicalAccessService.resolveTargetElderProfileId(principal, reminder.getElderProfileId());

        // 更新可编辑字段
        reminder.setTitle(request.getTitle().trim());
        reminder.setMedicineName(request.getMedicineName().trim());
        reminder.setDosage(trimToNull(request.getDosage()));
        reminder.setFrequencyRule(defaultRule(trimToNull(request.getFrequencyRule())));
        reminder.setRepeatRule(defaultRule(trimToNull(request.getRepeatRule())));
        reminder.setRemindTime(instantToShanghaiWall(request.getRemindTime()));
        reminder.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        reminder.setRelatedEventId(request.getRelatedEventId());
        reminder.setUpdatedAt(LocalDateTime.now(DISPLAY_ZONE));

        reminder = medicineReminderRepository.save(reminder);
        return toViewDto(reminder);
    }

    @Transactional
    public void deleteMedicineReminder(AuthPrincipal principal, Long reminderId) {
        // 查询提醒记录
        ChildMedicineReminder reminder = medicineReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ApiException(4004, "吃药提醒不存在"));

        // 验证权限：确保子女可以访问该老人的记录
        medicalAccessService.resolveTargetElderProfileId(principal, reminder.getElderProfileId());

        // 删除记录
        medicineReminderRepository.delete(reminder);
    }

    private static MedicineReminderViewDto toViewDto(ChildMedicineReminder reminder) {
        return MedicineReminderViewDto.builder()
                .id(reminder.getId())
                .elderProfileId(reminder.getElderProfileId())
                .title(reminder.getTitle())
                .medicineName(reminder.getMedicineName())
                .dosage(reminder.getDosage())
                .frequencyRule(reminder.getFrequencyRule())
                .repeatRule(reminder.getRepeatRule())
                .enabled(reminder.getEnabled())
                .remindTime(formatLocalTime(reminder.getRemindTime()))
                .sourceType(reminder.getSourceType())
                .status(reminder.getStatus())
                .createdBy(reminder.getCreatedBy())
                .relatedEventId(reminder.getRelatedEventId())
                .createdAt(formatLocalTime(reminder.getCreatedAt()))
                .updatedAt(formatLocalTime(reminder.getUpdatedAt()))
                .build();
    }

    /**
     * 写入 DATETIME：不带时区的「北京时间」墙上钟表。
     * <p>
     * 与 {@code hibernate.jdbc.time_zone=Asia/Shanghai}、连接串 {@code serverTimezone=Asia/Shanghai} 一致，
     * 避免「先按 UTC 落成 LocalDateTime 再被 JDBC 当作上海时间二次偏移」导致差 8 小时。
     */
    private static LocalDateTime instantToShanghaiWall(Instant instant) {
        if (instant == null) {
            throw new ApiException(4001, "remindTime 不能为空");
        }
        return LocalDateTime.ofInstant(instant, DISPLAY_ZONE);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String defaultRule(String value) {
        return value == null ? "none" : value;
    }

    /** API 字符串：库内已是北京时间分量，直接格式化 */
    private static String formatLocalTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(ISO_LOCAL_FORMATTER);
    }
}
