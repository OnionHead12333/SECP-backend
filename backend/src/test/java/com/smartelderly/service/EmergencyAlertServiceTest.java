package com.smartelderly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.CreateEmergencyRequest;
import com.smartelderly.api.dto.HandleEmergencyRequest;
import com.smartelderly.api.dto.RevokeRequest;
import com.smartelderly.domain.AlertStatus;
import com.smartelderly.domain.AlertType;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.CancelMode;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.EmergencyAlert;
import com.smartelderly.domain.EmergencyAlertRepository;
import com.smartelderly.domain.FamilyBinding;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.TriggerMode;
import com.smartelderly.util.TimeUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("紧急告警服务单元测试")
class EmergencyAlertServiceTest {

    @Mock
    private EmergencyAlertRepository alertRepository;

    @Mock
    private ElderProfileRepository elderProfileRepository;

    @Mock
    private FamilyBindingRepository familyBindingRepository;

    @InjectMocks
    private EmergencyAlertService emergencyAlertService;

    @Test
    @DisplayName("老人发起 SOS：无待撤销告警时应创建新告警")
    void createForElder_noPendingAlert_createsNewAlert() {
        ElderProfile elder = elder(7L, 100L, "张三");
        when(elderProfileRepository.findByClaimedUserId(100L)).thenReturn(Optional.of(elder));
        when(alertRepository.findFirstByElderProfileIdAndStatusOrderByIdDesc(7L, AlertStatus.pending_revoke))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(EmergencyAlert.class))).thenAnswer(invocation -> {
            EmergencyAlert alert = invocation.getArgument(0);
            alert.setId(55L);
            return alert;
        });

        var response = emergencyAlertService.createForElder(
                100L,
                new CreateEmergencyRequest(AlertType.sos, TriggerMode.button, 9L, "测试 SOS"));

        assertEquals(0, response.getCode());
        assertEquals("created", response.getMessage());
        assertEquals(55L, response.getData().get("alertId"));
        assertEquals(AlertStatus.pending_revoke.name(), response.getData().get("status"));

        ArgumentCaptor<EmergencyAlert> captor = ArgumentCaptor.forClass(EmergencyAlert.class);
        verify(alertRepository).save(captor.capture());
        EmergencyAlert saved = captor.getValue();
        assertEquals(7L, saved.getElderProfileId());
        assertEquals(AlertType.sos, saved.getAlertType());
        assertEquals(TriggerMode.button, saved.getTriggerMode());
        assertEquals(9L, saved.getLocationId());
        assertEquals("测试 SOS", saved.getRemark());
    }

    @Test
    @DisplayName("老人发起 SOS：已有待撤销告警时应返回现有告警")
    void createForElder_existingPendingAlert_returnsExistingAlert() {
        ElderProfile elder = elder(7L, 100L, "张三");
        EmergencyAlert existing = alert(55L, 7L, AlertStatus.pending_revoke);
        existing.setRevokeDeadline(TimeUtils.now().plusSeconds(30));

        when(elderProfileRepository.findByClaimedUserId(100L)).thenReturn(Optional.of(elder));
        when(alertRepository.findFirstByElderProfileIdAndStatusOrderByIdDesc(7L, AlertStatus.pending_revoke))
                .thenReturn(Optional.of(existing), Optional.of(existing));

        var response = emergencyAlertService.createForElder(
                100L,
                new CreateEmergencyRequest(AlertType.sos, TriggerMode.button, null, null));

        assertEquals("existing pending alert", response.getMessage());
        assertEquals(55L, response.getData().get("alertId"));
        verify(alertRepository, never()).save(argThat(a -> a.getId() == null));
    }

    @Test
    @DisplayName("老人撤回 SOS：pending_revoke 应变为 cancelled")
    void revokeForElder_pendingAlert_cancelsAlert() {
        ElderProfile elder = elder(7L, 100L, "张三");
        EmergencyAlert alert = alert(55L, 7L, AlertStatus.pending_revoke);
        LocalDateTime now = TimeUtils.now();
        alert.setRevokeDeadline(now.plusSeconds(30));

        when(elderProfileRepository.findByClaimedUserId(100L)).thenReturn(Optional.of(elder));
        when(alertRepository.findByIdAndElderProfileId(55L, 7L)).thenReturn(Optional.of(alert));

        var response = emergencyAlertService.revokeForElder(
                100L,
                55L,
                new RevokeRequest(CancelMode.button));

        assertEquals("revoked", response.getMessage());
        assertEquals(AlertStatus.cancelled.name(), response.getData().get("status"));
        assertEquals(CancelMode.button, alert.getCancelMode());
        assertNotNull(alert.getCancelTime());
        verify(alertRepository).save(alert);
    }

    @Test
    @DisplayName("老人立即发送 SOS：pending_revoke 应变为 sent")
    void sendNowForElder_pendingAlert_sendsAlert() {
        ElderProfile elder = elder(7L, 100L, "张三");
        EmergencyAlert alert = alert(55L, 7L, AlertStatus.pending_revoke);
        LocalDateTime now = TimeUtils.now();
        alert.setRevokeDeadline(now.plusSeconds(30));

        when(elderProfileRepository.findByClaimedUserId(100L)).thenReturn(Optional.of(elder));
        when(alertRepository.findByIdAndElderProfileId(55L, 7L)).thenReturn(Optional.of(alert));

        var response = emergencyAlertService.sendNowForElder(100L, 55L);

        assertEquals("sent", response.getMessage());
        assertEquals(AlertStatus.sent.name(), response.getData().get("status"));
        assertNotNull(alert.getSentTime());
        verify(alertRepository).save(alert);
    }

    @Test
    @DisplayName("老人查看 SOS：超过撤销截止时间时应自动转为 sent")
    void getForElder_expiredPendingAlert_autoSendsBeforeReturn() {
        ElderProfile elder = elder(7L, 100L, "张三");
        EmergencyAlert alert = alert(55L, 7L, AlertStatus.pending_revoke);
        alert.setRevokeDeadline(TimeUtils.now().minusSeconds(1));

        when(elderProfileRepository.findByClaimedUserId(100L)).thenReturn(Optional.of(elder));
        when(alertRepository.findByIdAndElderProfileId(55L, 7L)).thenReturn(Optional.of(alert));

        var response = emergencyAlertService.getForElder(100L, 55L);

        assertEquals(0, response.getCode());
        assertEquals(AlertStatus.sent.name(), response.getData().get("status"));
        assertNotNull(alert.getSentTime());
        verify(alertRepository).save(alert);
    }

    @Test
    @DisplayName("子女查询告警列表：无绑定老人时返回空页")
    void listForChild_noBindings_returnsEmptyPage() {
        when(familyBindingRepository.findByChildUserIdAndStatus(200L, BindingStatus.active))
                .thenReturn(List.of());

        var response = emergencyAlertService.listForChild(200L, null, 1, 20);

        assertEquals(0, response.getCode());
        Map<String, Object> page = response.getData();
        assertEquals(List.of(), page.get("list"));
        assertNumberEquals(0L, page.get("total"));
        verifyNoInteractions(alertRepository);
    }

    @Test
    @DisplayName("子女查询告警列表：非法状态筛选应抛出业务异常")
    void listForChild_invalidStatus_throwsApiException() {
        when(familyBindingRepository.findByChildUserIdAndStatus(200L, BindingStatus.active))
                .thenReturn(List.of(binding(7L, 200L)));

        assertThrows(ApiException.class,
                () -> emergencyAlertService.listForChild(200L, "pending_revoke", 1, 20));
    }

    @Test
    @DisplayName("子女查询告警列表：应返回绑定老人 sent/handled 告警")
    void listForChild_boundElders_returnsPagedAlerts() {
        EmergencyAlert sent = alert(55L, 7L, AlertStatus.sent);
        sent.setSentTime(TimeUtils.now());

        when(familyBindingRepository.findByChildUserIdAndStatus(200L, BindingStatus.active))
                .thenReturn(List.of(binding(7L, 200L)));
        when(alertRepository.findByElderProfileIdInAndStatusIn(
                any(Collection.class), any(Collection.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sent)));
        when(elderProfileRepository.findAllById(Set.of(7L))).thenReturn(List.of(elder(7L, 100L, "张三")));

        var response = emergencyAlertService.listForChild(200L, null, 0, 200);

        Map<String, Object> page = response.getData();
        assertNumberEquals(1L, page.get("page"));
        assertNumberEquals(100L, page.get("pageSize"));
        assertNumberEquals(1L, page.get("total"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) page.get("list");
        assertEquals("张三", rows.get(0).get("elderName"));
        assertEquals(AlertStatus.sent.name(), rows.get(0).get("status"));
    }

    @Test
    @DisplayName("子女处理 SOS：handled 动作应更新状态")
    void handleForChild_validHandledAction_updatesAlert() {
        EmergencyAlert sent = alert(55L, 7L, AlertStatus.sent);
        when(familyBindingRepository.findByChildUserIdAndStatus(200L, BindingStatus.active))
                .thenReturn(List.of(binding(7L, 200L)));
        when(alertRepository.findById(55L)).thenReturn(Optional.of(sent));

        var response = emergencyAlertService.handleForChild(
                200L,
                55L,
                new HandleEmergencyRequest("handled", "已电话确认"));

        assertEquals("handled", response.getMessage());
        assertEquals(AlertStatus.handled, sent.getStatus());
        assertEquals(200L, sent.getHandledBy());
        assertEquals("已电话确认", sent.getRemark());
        verify(alertRepository).save(sent);
    }

    @Test
    @DisplayName("子女处理 SOS：未绑定老人告警应拒绝访问")
    void handleForChild_unboundAlert_throwsApiException() {
        EmergencyAlert sent = alert(55L, 7L, AlertStatus.sent);
        when(familyBindingRepository.findByChildUserIdAndStatus(200L, BindingStatus.active))
                .thenReturn(List.of(binding(8L, 200L)));
        when(alertRepository.findById(55L)).thenReturn(Optional.of(sent));

        assertThrows(ApiException.class,
                () -> emergencyAlertService.handleForChild(
                        200L,
                        55L,
                        new HandleEmergencyRequest("handled", null)));
    }

    private static ElderProfile elder(Long id, Long claimedUserId, String name) {
        ElderProfile elder = new ElderProfile();
        elder.setId(id);
        elder.setClaimedUserId(claimedUserId);
        elder.setName(name);
        elder.setPhone("13900000000");
        return elder;
    }

    private static EmergencyAlert alert(Long id, Long elderProfileId, AlertStatus status) {
        EmergencyAlert alert = new EmergencyAlert();
        alert.setId(id);
        alert.setElderProfileId(elderProfileId);
        alert.setAlertType(AlertType.sos);
        alert.setTriggerMode(TriggerMode.button);
        alert.setStatus(status);
        alert.setTriggerTime(TimeUtils.now().minusSeconds(5));
        return alert;
    }

    private static FamilyBinding binding(Long elderProfileId, Long childUserId) {
        FamilyBinding binding = new FamilyBinding();
        binding.setElderProfileId(elderProfileId);
        binding.setChildUserId(childUserId);
        binding.setStatus(BindingStatus.active);
        binding.setIsPrimary(true);
        binding.setRelation("子女");
        return binding;
    }

    private static void assertNumberEquals(long expected, Object actual) {
        assertTrue(actual instanceof Number, "actual value should be numeric");
        assertEquals(expected, ((Number) actual).longValue());
    }
}
