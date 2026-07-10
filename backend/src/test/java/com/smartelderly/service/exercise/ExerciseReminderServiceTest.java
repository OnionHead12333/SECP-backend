package com.smartelderly.service.exercise;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.exercise.dto.ExerciseReminderCreateRequest;
import com.smartelderly.api.exercise.dto.ExerciseReminderUpdateRequest;
import com.smartelderly.domain.exercise.ExerciseReminder;
import com.smartelderly.domain.exercise.ExerciseReminderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("锻炼提醒服务单元测试")
class ExerciseReminderServiceTest {

    @Mock
    private ExerciseReminderRepository exerciseReminderRepository;

    @InjectMocks
    private ExerciseReminderService service;

    @Test
    @DisplayName("查询锻炼提醒列表：应映射响应 DTO")
    void listByElderProfileId_returnsMappedResponses() {
        when(exerciseReminderRepository.findByElderProfileIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(reminder(1L, 7L, "散步")));

        var result = service.listByElderProfileId(7L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("散步", result.get(0).getTitle());
        assertEquals("walk", result.get(0).getExerciseType());
        assertEquals("08:00:00", result.get(0).getStartTime());
    }

    @Test
    @DisplayName("创建锻炼提醒：应补齐固定字段并保存")
    void create_validRequest_savesReminderWithDefaults() {
        ExerciseReminderCreateRequest request = new ExerciseReminderCreateRequest();
        request.setElderProfileId(7L);
        request.setTitle("晨练");
        request.setExerciseType("walk");
        request.setStartTime("08:00:00");
        request.setEndTime("08:30:00");
        request.setRepeatRule("daily");
        request.setRemindTime("ignored");

        when(exerciseReminderRepository.save(any(ExerciseReminder.class))).thenAnswer(invocation -> {
            ExerciseReminder reminder = invocation.getArgument(0);
            reminder.setId(10L);
            return reminder;
        });

        var response = service.create(request);

        assertEquals(10L, response.getId());
        assertEquals("晨练", response.getTitle());
        assertEquals("pending", response.getStatus());
        verify(exerciseReminderRepository).save(argThat(reminder ->
                reminder.getEnabled()
                        && "child_remote".equals(reminder.getSourceType())
                        && "child".equals(reminder.getCreatedBy())
                        && reminder.getRemindTime() != null));
    }

    @Test
    @DisplayName("创建锻炼提醒：开始时间为空时应抛出业务异常")
    void create_missingStartTime_throwsApiException() {
        ExerciseReminderCreateRequest request = new ExerciseReminderCreateRequest();
        request.setElderProfileId(7L);
        request.setTitle("晨练");
        request.setExerciseType("walk");
        request.setRepeatRule("daily");

        assertThrows(ApiException.class, () -> service.create(request));
        verify(exerciseReminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新锻炼提醒：存在记录时应更新字段")
    void update_existingReminder_updatesFields() {
        ExerciseReminder existing = reminder(1L, 7L, "旧散步");
        ExerciseReminderUpdateRequest request = new ExerciseReminderUpdateRequest();
        request.setTitle("新散步");
        request.setExerciseType("stretch");
        request.setRepeatRule("weekly");
        request.setStartTime("09:00:00");
        request.setEndTime("09:30:00");

        when(exerciseReminderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(exerciseReminderRepository.save(any(ExerciseReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(1L, request);

        assertEquals("新散步", response.getTitle());
        assertEquals("stretch", response.getExerciseType());
        assertEquals("weekly", response.getRepeatRule());
        assertEquals("09:00:00", response.getStartTime());
    }

    @Test
    @DisplayName("删除锻炼提醒：不存在时应抛出业务异常")
    void delete_missingReminder_throwsApiException() {
        when(exerciseReminderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.delete(404L));
    }

    private static ExerciseReminder reminder(Long id, Long elderProfileId, String title) {
        ExerciseReminder reminder = new ExerciseReminder();
        reminder.setId(id);
        reminder.setElderProfileId(elderProfileId);
        reminder.setTitle(title);
        reminder.setExerciseType("walk");
        reminder.setStartTime(LocalTime.of(8, 0));
        reminder.setEndTime(LocalTime.of(8, 30));
        reminder.setRepeatRule("daily");
        reminder.setRemindTime(LocalDateTime.now().plusHours(1));
        reminder.setSourceType("child_remote");
        reminder.setStatus("pending");
        reminder.setCreatedBy("child");
        reminder.setEnabled(true);
        return reminder;
    }
}
