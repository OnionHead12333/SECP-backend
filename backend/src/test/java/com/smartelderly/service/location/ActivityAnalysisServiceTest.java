package com.smartelderly.service.location;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.domain.AlertStatus;
import com.smartelderly.domain.AlertType;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.EmergencyAlert;
import com.smartelderly.domain.EmergencyAlertRepository;
import com.smartelderly.domain.TriggerMode;
import com.smartelderly.domain.location.ElderGuardRule;
import com.smartelderly.domain.location.ElderGuardRuleRepository;
import com.smartelderly.domain.location.Geofence;
import com.smartelderly.domain.location.GeofenceRepository;
import com.smartelderly.domain.location.LocationLog;
import com.smartelderly.domain.location.LocationLogRepository;

/**
 * ActivityAnalysisService 的单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("活动状态分析服务测试")
public class ActivityAnalysisServiceTest {

    @Mock
    private LocationLogRepository locationLogRepository;

    @Mock
    private GeofenceRepository geofenceRepository;

    @Mock
    private ElderGuardRuleRepository guardRuleRepository;

    @Mock
    private ElderProfileRepository elderProfileRepository;

    @Mock
    private EmergencyAlertRepository emergencyAlertRepository;

    @Mock
    private GeofenceAnalysisHelper geofenceAnalysisHelper;

    @InjectMocks
    private ActivityAnalysisService activityAnalysisService;

    private ElderGuardRule rule;
    private ElderProfile elder;
    private LocationLog location;
    private List<Geofence> geofences;

    @BeforeEach
    public void setUp() {
        // 初始化监护规则
        rule = new ElderGuardRule();
        rule.setId(1L);
        rule.setElderProfileId(1L);
        rule.setEnabled(true);
        rule.setActiveStartTime(LocalTime.of(8, 0, 0));
        rule.setActiveEndTime(LocalTime.of(18, 0, 0));
        rule.setHomeInactivityMinutes(120);
        rule.setOutsideInactivityMinutes(60);
        rule.setAlertMinIntervalMinutes(120);

        // 初始化老人
        elder = new ElderProfile();
        elder.setId(1L);

        // 初始化位置
        location = new LocationLog();
        location.setElderProfileId(1L);
        location.setLatitude(new BigDecimal("31.230500"));
        location.setLongitude(new BigDecimal("121.473900"));
        location.setRecordedAt(LocalDateTime.now().minusMinutes(150)); // 150分钟前

        // 初始化围栏
        geofences = new ArrayList<>();
    }

    @Test
    @DisplayName("测试：运行分析任务 - 无启用规则")
    public void testRunAnalysisJob_NoEnabledRules() {
        // 给定：没有启用的监护规则
        when(guardRuleRepository.findByEnabled(true)).thenReturn(new ArrayList<>());

        // 当：运行分析任务
        int alertCount = activityAnalysisService.runAnalysisJob();

        // 那么：应该返回 0，不生成任何告警
        assertEquals(0, alertCount);
        verify(emergencyAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("测试：运行分析任务 - 多个启用规则")
    public void testRunAnalysisJob_MultipleEnabledRules() {
        // 给定：两个启用的规则
        ElderGuardRule rule2 = new ElderGuardRule();
        rule2.setId(2L);
        rule2.setElderProfileId(2L);
        rule2.setEnabled(true);
        rule2.setActiveStartTime(LocalTime.of(8, 0, 0));
        rule2.setActiveEndTime(LocalTime.of(18, 0, 0));
        rule2.setHomeInactivityMinutes(120);
        rule2.setOutsideInactivityMinutes(60);
        rule2.setAlertMinIntervalMinutes(120);

        List<ElderGuardRule> rules = List.of(rule, rule2);
        when(guardRuleRepository.findByEnabled(true)).thenReturn(rules);

        // 第一个老人存在，有位置，超过阈值，在家
        when(elderProfileRepository.findById(1L)).thenReturn(Optional.of(elder));
        location.setRecordedAt(LocalDateTime.now().minusMinutes(150));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(location));
        when(geofenceRepository.findByElderProfileIdAndIsEnabled(1L, true))
                .thenReturn(geofences);
        when(geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences))
                .thenReturn(true);
        when(emergencyAlertRepository.findByElderProfileIdAndAlertTypeAndTriggerModeAndTriggerTimeAfter(
                eq(1L), eq(AlertType.inactivity), eq(TriggerMode.rule_engine), any()))
                .thenReturn(new ArrayList<>());

        // 第二个老人不存在
        when(elderProfileRepository.findById(2L)).thenReturn(Optional.empty());

        // 当：运行分析任务
        int alertCount = activityAnalysisService.runAnalysisJob();

        // 那么：应该生成 1 条告警（第一个老人），第二个老人被跳过
        assertEquals(1, alertCount);
        verify(emergencyAlertRepository, times(1)).save(any(EmergencyAlert.class));
    }

    @Test
    @DisplayName("测试：运行分析任务 - 在家且未超过阈值")
    public void testRunAnalysisJob_AtHomeWithinThreshold() {
        // 给定：位置在家，但未活动时长未超过 homeInactivityMinutes（120分钟）
        location.setRecordedAt(LocalDateTime.now().minusMinutes(60)); // 只有 60 分钟未更新
        List<ElderGuardRule> rules = List.of(rule);

        when(guardRuleRepository.findByEnabled(true)).thenReturn(rules);
        when(elderProfileRepository.findById(1L)).thenReturn(Optional.of(elder));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(location));
        when(geofenceRepository.findByElderProfileIdAndIsEnabled(1L, true))
                .thenReturn(geofences);
        when(geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences))
                .thenReturn(true); // 在家

        // 当：运行分析任务
        int alertCount = activityAnalysisService.runAnalysisJob();

        // 那么：不应生成告警（因为还没超过 120 分钟）
        assertEquals(0, alertCount);
        verify(emergencyAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("测试：运行分析任务 - 在家且超过阈值，生成告警")
    public void testRunAnalysisJob_AtHomeExceedsThreshold() {
        // 给定：位置在家，未活动时长超过 homeInactivityMinutes（120分钟）
        location.setRecordedAt(LocalDateTime.now().minusMinutes(150)); // 150 分钟未更新
        List<ElderGuardRule> rules = List.of(rule);

        when(guardRuleRepository.findByEnabled(true)).thenReturn(rules);
        when(elderProfileRepository.findById(1L)).thenReturn(Optional.of(elder));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(location));
        when(geofenceRepository.findByElderProfileIdAndIsEnabled(1L, true))
                .thenReturn(geofences);
        when(geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences))
                .thenReturn(true); // 在家
        when(emergencyAlertRepository.findByElderProfileIdAndAlertTypeAndTriggerModeAndTriggerTimeAfter(
                eq(1L), eq(AlertType.inactivity), eq(TriggerMode.rule_engine), any()))
                .thenReturn(new ArrayList<>()); // 没有最近的相同告警

        // 当：运行分析任务
        int alertCount = activityAnalysisService.runAnalysisJob();

        // 那么：应该生成 1 条告警
        assertEquals(1, alertCount);
        verify(emergencyAlertRepository, times(1)).save(any(EmergencyAlert.class));

        // 验证保存的告警内容
        ArgumentCaptor<EmergencyAlert> captor = ArgumentCaptor.forClass(EmergencyAlert.class);
        verify(emergencyAlertRepository).save(captor.capture());
        EmergencyAlert savedAlert = captor.getValue();

        assertEquals(1L, savedAlert.getElderProfileId());
        assertEquals(AlertType.inactivity, savedAlert.getAlertType());
        assertEquals(TriggerMode.rule_engine, savedAlert.getTriggerMode());
        assertEquals(AlertStatus.pending_revoke, savedAlert.getStatus());
        assertNotNull(savedAlert.getTriggerTime());
        assertNotNull(savedAlert.getRevokeDeadline());
    }

    @Test
    @DisplayName("测试：运行分析任务 - 外出且超过阈值，生成告警")
    public void testRunAnalysisJob_AwayExceedsThreshold() {
        // 给定：位置外出，未活动时长超过 outsideInactivityMinutes（60分钟）
        location.setRecordedAt(LocalDateTime.now().minusMinutes(90)); // 90 分钟未更新
        List<ElderGuardRule> rules = List.of(rule);

        when(guardRuleRepository.findByEnabled(true)).thenReturn(rules);
        when(elderProfileRepository.findById(1L)).thenReturn(Optional.of(elder));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(location));
        when(geofenceRepository.findByElderProfileIdAndIsEnabled(1L, true))
                .thenReturn(geofences);
        when(geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences))
                .thenReturn(false); // 外出
        when(emergencyAlertRepository.findByElderProfileIdAndAlertTypeAndTriggerModeAndTriggerTimeAfter(
                eq(1L), eq(AlertType.inactivity), eq(TriggerMode.rule_engine), any()))
                .thenReturn(new ArrayList<>()); // 没有最近的相同告警

        // 当：运行分析任务
        int alertCount = activityAnalysisService.runAnalysisJob();

        // 那么：应该生成 1 条告警
        assertEquals(1, alertCount);
        verify(emergencyAlertRepository, times(1)).save(any(EmergencyAlert.class));
    }

    @Test
    @DisplayName("测试：运行分析任务 - 告警最小间隔限制")
    public void testRunAnalysisJob_AlertMinIntervalRestriction() {
        // 给定：位置在家，超过阈值，但最近已生成过相同告警
        location.setRecordedAt(LocalDateTime.now().minusMinutes(150));
        List<ElderGuardRule> rules = List.of(rule);

        EmergencyAlert recentAlert = new EmergencyAlert();
        recentAlert.setTriggerTime(LocalDateTime.now().minusMinutes(60)); // 60分钟前生成

        when(guardRuleRepository.findByEnabled(true)).thenReturn(rules);
        when(elderProfileRepository.findById(1L)).thenReturn(Optional.of(elder));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(location));
        when(geofenceRepository.findByElderProfileIdAndIsEnabled(1L, true))
                .thenReturn(geofences);
        when(geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences))
                .thenReturn(true);
        when(emergencyAlertRepository.findByElderProfileIdAndAlertTypeAndTriggerModeAndTriggerTimeAfter(
                eq(1L), eq(AlertType.inactivity), eq(TriggerMode.rule_engine), any()))
                .thenReturn(List.of(recentAlert)); // 有最近的相同告警

        // 当：运行分析任务
        int alertCount = activityAnalysisService.runAnalysisJob();

        // 那么：不应生成新告警（因为在最小间隔内已有相同告警）
        assertEquals(0, alertCount);
        verify(emergencyAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("测试：运行分析任务 - 老人不存在时跳过")
    public void testRunAnalysisJob_ElderNotFound() {
        // 给定：监护规则对应的老人不存在
        List<ElderGuardRule> rules = List.of(rule);
        when(guardRuleRepository.findByEnabled(true)).thenReturn(rules);
        when(elderProfileRepository.findById(1L)).thenReturn(Optional.empty());

        // 当：运行分析任务
        int alertCount = activityAnalysisService.runAnalysisJob();

        // 那么：应该返回 0，跳过该规则
        assertEquals(0, alertCount);
        verify(locationLogRepository, never()).findFirstByElderProfileIdOrderByRecordedAtDesc(any());
    }
}
