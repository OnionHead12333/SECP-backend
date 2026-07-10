package com.smartelderly.service.medical;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.medical.dto.ChildMedicineReminderCreateRequest;
import com.smartelderly.api.medical.dto.ChildMedicineReminderUpdateRequest;
import com.smartelderly.domain.UserRole;
import com.smartelderly.domain.medical.ChildMedicineReminder;
import com.smartelderly.domain.medical.ChildMedicineReminderRepository;
import com.smartelderly.security.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("子女用药提醒服务单元测试")
class ChildMedicineReminderServiceTest {

    @Mock
    private MedicalAccessService medicalAccessService;

    @Mock
    private ChildMedicineReminderRepository medicineReminderRepository;

    @InjectMocks
    private ChildMedicineReminderService service;

    private final AuthPrincipal child = new AuthPrincipal(20L, UserRole.child);

    @Test
    @DisplayName("查询用药提醒列表：应先解析目标老人再按提醒时间排序返回")
    void listMedicineReminders_resolvesAccessAndMapsDtos() {
        when(medicalAccessService.resolveTargetElderProfileId(child, 7L)).thenReturn(7L);
        when(medicineReminderRepository.findByElderProfileIdOrderByRemindTimeAsc(7L))
                .thenReturn(List.of(reminder(1L, 7L, "降压药")));

        var result = service.listMedicineReminders(child, 7L);

        assertEquals(1, result.size());
        assertEquals("降压药", result.get(0).getMedicineName());
        assertEquals("none", result.get(0).getRepeatRule());
    }

    @Test
    @DisplayName("创建用药提醒：应 trim 字段、默认规则并保存")
    void createMedicineReminder_validRequest_savesReminder() {
        ChildMedicineReminderCreateRequest request = new ChildMedicineReminderCreateRequest();
        request.setElderProfileId(7L);
        request.setTitle(" 早餐后 ");
        request.setMedicineName(" 降压药 ");
        request.setDosage(" 1片 ");
        request.setFrequencyRule(" ");
        request.setRepeatRule(null);
        request.setRemindTime(Instant.parse("2026-06-06T10:00:00Z"));
        request.setEnabled(true);
        request.setRelatedEventId(99L);

        when(medicalAccessService.resolveTargetElderProfileId(child, 7L)).thenReturn(7L);
        when(medicineReminderRepository.save(any(ChildMedicineReminder.class))).thenAnswer(invocation -> {
            ChildMedicineReminder reminder = invocation.getArgument(0);
            reminder.setId(10L);
            return reminder;
        });

        var response = service.createMedicineReminder(child, request);

        assertEquals(10L, response.getId());
        assertEquals("早餐后", response.getTitle());
        assertEquals("降压药", response.getMedicineName());
        assertEquals("1片", response.getDosage());
        assertEquals("none", response.getFrequencyRule());
        assertEquals("none", response.getRepeatRule());
        assertEquals("child_remote", response.getSourceType());
        assertEquals("pending", response.getStatus());
        assertEquals("2026-06-06T18:00:00", response.getRemindTime());
    }

    @Test
    @DisplayName("创建用药提醒：提醒时间为空时应抛出业务异常")
    void createMedicineReminder_nullRemindTime_throwsApiException() {
        ChildMedicineReminderCreateRequest request = new ChildMedicineReminderCreateRequest();
        request.setElderProfileId(7L);
        request.setTitle("早餐后");
        request.setMedicineName("降压药");
        request.setEnabled(true);
        when(medicalAccessService.resolveTargetElderProfileId(child, 7L)).thenReturn(7L);

        assertThrows(ApiException.class, () -> service.createMedicineReminder(child, request));
    }

    @Test
    @DisplayName("更新用药提醒：存在记录时应验证权限并保存")
    void updateMedicineReminder_existingReminder_updatesFields() {
        ChildMedicineReminder existing = reminder(1L, 7L, "旧药");
        ChildMedicineReminderUpdateRequest request = new ChildMedicineReminderUpdateRequest();
        request.setTitle(" 晚餐后 ");
        request.setMedicineName(" 阿司匹林 ");
        request.setDosage("");
        request.setFrequencyRule("daily");
        request.setRepeatRule("daily");
        request.setRemindTime(Instant.parse("2026-06-06T11:00:00Z"));
        request.setEnabled(false);
        request.setRelatedEventId(8L);

        when(medicineReminderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(medicalAccessService.resolveTargetElderProfileId(child, 7L)).thenReturn(7L);
        when(medicineReminderRepository.save(any(ChildMedicineReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateMedicineReminder(child, 1L, request);

        assertEquals("晚餐后", response.getTitle());
        assertEquals("阿司匹林", response.getMedicineName());
        assertNull(response.getDosage());
        assertFalse(response.getEnabled());
        assertEquals("2026-06-06T19:00:00", response.getRemindTime());
    }

    @Test
    @DisplayName("删除用药提醒：不存在时应抛出业务异常")
    void deleteMedicineReminder_missingReminder_throwsApiException() {
        when(medicineReminderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.deleteMedicineReminder(child, 404L));
        verify(medicineReminderRepository, never()).delete(any());
    }

    private static ChildMedicineReminder reminder(Long id, Long elderProfileId, String medicineName) {
        ChildMedicineReminder reminder = new ChildMedicineReminder();
        reminder.setId(id);
        reminder.setElderProfileId(elderProfileId);
        reminder.setTitle("早餐后");
        reminder.setMedicineName(medicineName);
        reminder.setDosage("1片");
        reminder.setFrequencyRule("none");
        reminder.setRepeatRule("none");
        reminder.setRemindTime(LocalDateTime.of(2026, 6, 6, 8, 0));
        reminder.setEnabled(true);
        reminder.setSourceType("child_remote");
        reminder.setStatus("pending");
        reminder.setCreatedBy("child");
        reminder.setCreatedAt(LocalDateTime.of(2026, 6, 6, 7, 0));
        reminder.setUpdatedAt(LocalDateTime.of(2026, 6, 6, 7, 0));
        return reminder;
    }
}
