package com.smartelderly.service;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.WaterReminderCreateRequest;
import com.smartelderly.api.dto.WaterReminderUpdateRequest;
import com.smartelderly.domain.ReminderExecutionLog;
import com.smartelderly.domain.ReminderExecutionLogRepository;
import com.smartelderly.domain.WaterReminder;
import com.smartelderly.domain.WaterReminderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("喝水提醒服务单元测试")
class WaterReminderServiceTest {

    @Mock
    private WaterReminderRepository waterReminderRepository;

    @Mock
    private ReminderExecutionLogRepository executionLogRepository;

    @InjectMocks
    private WaterReminderService waterReminderService;

    @Test
    @DisplayName("查询老人喝水提醒列表：应按响应 DTO 映射")
    void listByElderProfileId_returnsMappedResponses() {
        WaterReminder reminder = reminder(10L, 7L, "晨间喝水");
        when(waterReminderRepository.findByElderProfileIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(reminder));

        var result = waterReminderService.listByElderProfileId(7L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(7L, result.get(0).getElderProfileId());
        assertEquals("晨间喝水", result.get(0).getTitle());
        assertEquals(1500, result.get(0).getDailyTargetMl());
        assertEquals("08:00:00", result.get(0).getStartTime());
        assertEquals("22:00:00", result.get(0).getEndTime());
    }

    @Test
    @DisplayName("创建喝水提醒：应解析时间并保存实体")
    void create_validRequest_savesReminder() {
        WaterReminderCreateRequest request = new WaterReminderCreateRequest();
        request.setElderProfileId(7L);
        request.setTitle("午间喝水");
        request.setDailyTargetMl(1800);
        request.setIntervalMinutes(90);
        request.setStartTime("09:00:00");
        request.setEndTime("21:00:00");
        request.setRemindTime("2026-06-06T10:00:00Z");
        request.setSourceType("child");
        request.setStatus("active");
        request.setCreatedBy("child");

        when(waterReminderRepository.save(any(WaterReminder.class))).thenAnswer(invocation -> {
            WaterReminder saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        var response = waterReminderService.create(100L, request);

        assertEquals(20L, response.getId());
        assertEquals(7L, response.getElderProfileId());
        assertEquals("午间喝水", response.getTitle());
        assertEquals(1800, response.getDailyTargetMl());
        assertEquals(90, response.getIntervalMinutes());
        assertEquals("09:00:00", response.getStartTime());
        assertEquals("21:00:00", response.getEndTime());

        ArgumentCaptor<WaterReminder> captor = ArgumentCaptor.forClass(WaterReminder.class);
        verify(waterReminderRepository).save(captor.capture());
        WaterReminder saved = captor.getValue();
        assertTrue(saved.getEnabled());
        assertEquals(LocalTime.of(9, 0), saved.getStartTime());
        assertEquals(LocalTime.of(21, 0), saved.getEndTime());
        assertEquals(LocalDateTime.of(2026, 6, 6, 18, 0), saved.getRemindTime());
    }

    @Test
    @DisplayName("更新喝水提醒：应保存新值并删除旧 pending 日志")
    void update_existingReminder_updatesAndDeletesPendingLogs() {
        WaterReminder existing = reminder(10L, 7L, "旧标题");
        WaterReminderUpdateRequest request = new WaterReminderUpdateRequest();
        request.setTitle("新标题");
        request.setDailyTargetMl(2000);
        request.setIntervalMinutes(120);
        request.setStartTime("10:00:00");
        request.setEndTime("20:00:00");

        ReminderExecutionLog pending = new ReminderExecutionLog();
        pending.setReminderId(10L);
        pending.setStatus("pending");

        when(waterReminderRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(waterReminderRepository.save(any(WaterReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(executionLogRepository.findByReminderIdAndStatus(10L, "pending")).thenReturn(List.of(pending));

        var response = waterReminderService.update(10L, request);

        assertEquals("新标题", response.getTitle());
        assertEquals(2000, response.getDailyTargetMl());
        assertEquals(120, response.getIntervalMinutes());
        assertEquals("10:00:00", response.getStartTime());
        assertEquals("20:00:00", response.getEndTime());
        verify(executionLogRepository).deleteAll(List.of(pending));
    }

    @Test
    @DisplayName("删除喝水提醒：提醒不存在时应抛出业务异常")
    void delete_missingReminder_throwsApiException() {
        when(waterReminderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> waterReminderService.delete(404L));

        verify(waterReminderRepository, never()).delete(any());
        verifyNoInteractions(executionLogRepository);
    }

    @Test
    @DisplayName("超时任务：过期 pending 日志应标记为 missed")
    void markTimedOutAsMissed_pendingLogsBeforeDeadline_marksMissed() {
        ReminderExecutionLog pending = new ReminderExecutionLog();
        pending.setStatus("pending");
        pending.setIsTimeout(false);

        when(executionLogRepository.findPendingWaterLogsBefore(any(), any()))
                .thenReturn(List.of(pending));

        waterReminderService.markTimedOutAsMissed();

        assertEquals("missed", pending.getStatus());
        assertTrue(pending.getIsTimeout());
        verify(executionLogRepository).save(pending);
    }

    private static WaterReminder reminder(Long id, Long elderProfileId, String title) {
        WaterReminder reminder = new WaterReminder();
        reminder.setId(id);
        reminder.setElderProfileId(elderProfileId);
        reminder.setTitle(title);
        reminder.setDailyTargetMl(1500);
        reminder.setIntervalMinutes(60);
        reminder.setStartTime(LocalTime.of(8, 0));
        reminder.setEndTime(LocalTime.of(22, 0));
        reminder.setTodayIntakeMl(0);
        reminder.setSourceType("child");
        reminder.setRemindTime(LocalDateTime.of(2026, 6, 6, 18, 0));
        reminder.setEnabled(true);
        reminder.setStatus("active");
        reminder.setCreatedBy("child");
        reminder.setCreatedAt(LocalDateTime.of(2026, 6, 6, 9, 0));
        reminder.setUpdatedAt(LocalDateTime.of(2026, 6, 6, 9, 0));
        return reminder;
    }
}
