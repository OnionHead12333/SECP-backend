package com.smartelderly.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.dto.CreateEmergencyRequest;
import com.smartelderly.api.dto.HandleEmergencyRequest;
import com.smartelderly.api.dto.RevokeRequest;
import com.smartelderly.domain.AlertStatus;
import com.smartelderly.domain.AlertType;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.EmergencyAlert;
import com.smartelderly.domain.EmergencyAlertRepository;
import com.smartelderly.domain.FamilyBinding;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.TriggerMode;
import com.smartelderly.util.TimeUtils;

@Service
public class EmergencyAlertService {

    private static final Set<String> CHILD_LIST_STATUS_FILTER = Set.of("sent", "handled", "false_alarm");

    private final EmergencyAlertRepository alertRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final FamilyBindingRepository familyBindingRepository;

    public EmergencyAlertService(
            EmergencyAlertRepository alertRepository,
            ElderProfileRepository elderProfileRepository,
            FamilyBindingRepository familyBindingRepository) {
        this.alertRepository = alertRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.familyBindingRepository = familyBindingRepository;
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createForElder(long userId, CreateEmergencyRequest req) {
        ElderProfile elder = elderProfileRepository
                .findByClaimedUserId(userId)
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));
        LocalDateTime now = TimeUtils.now();
        alertRepository
                .findFirstByElderProfileIdAndStatusOrderByIdDesc(elder.getId(), AlertStatus.pending_revoke)
                .ifPresent(a -> maybeAutoSend(a, now));

        return alertRepository
                .findFirstByElderProfileIdAndStatusOrderByIdDesc(elder.getId(), AlertStatus.pending_revoke)
                .map(existing -> ApiResponse.ok("existing pending alert", toCreatedBody(existing, now)))
                .orElseGet(() -> {
                    EmergencyAlert a = new EmergencyAlert();
                    a.setElderProfileId(elder.getId());
                    a.setAlertType(req.alertType() != null ? req.alertType() : AlertType.sos);
                    a.setTriggerMode(req.triggerMode() != null ? req.triggerMode() : TriggerMode.button);
                    a.setLocationId(req.locationId());
                    a.setRemark(req.remark());
                    a.setStatus(AlertStatus.pending_revoke);
                    a.setTriggerTime(now);
                    a.setRevokeDeadline(now.plusSeconds(5));
                    alertRepository.save(a);
                    return ApiResponse.ok("created", toCreatedBody(a, now));
                });
    }

    @Transactional
    public ApiResponse<Map<String, Object>> revokeForElder(long userId, long alertId, RevokeRequest req) {
        ElderProfile elder = elderProfileRepository
                .findByClaimedUserId(userId)
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));
        EmergencyAlert a = alertRepository
                .findByIdAndElderProfileId(alertId, elder.getId())
                .orElseThrow(() -> new ApiException(4004, "alert not found"));
        LocalDateTime now = TimeUtils.now();
        LocalDateTime deadline = a.getRevokeDeadline();
        maybeAutoSend(a, now);
        if (a.getStatus() != AlertStatus.pending_revoke) {
            if (a.getStatus() == AlertStatus.sent && deadline != null && !now.isBefore(deadline)) {
                throw new ApiException(4001, "revoke window expired");
            }
            throw new ApiException(4002, "alert status does not allow revoke");
        }
        a.setStatus(AlertStatus.cancelled);
        a.setCancelTime(now);
        a.setCancelMode(req.cancelMode());
        alertRepository.save(a);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alertId", a.getId());
        data.put("status", a.getStatus().name());
        data.put("cancelTime", TimeUtils.toInstant(a.getCancelTime()));
        data.put("cancelMode", a.getCancelMode().name());
        return ApiResponse.ok("revoked", data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> sendNowForElder(long userId, long alertId) {
        ElderProfile elder = elderProfileRepository
                .findByClaimedUserId(userId)
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));
        EmergencyAlert a = alertRepository
                .findByIdAndElderProfileId(alertId, elder.getId())
                .orElseThrow(() -> new ApiException(4004, "alert not found"));
        LocalDateTime now = TimeUtils.now();
        maybeAutoSend(a, now);
        if (a.getStatus() != AlertStatus.pending_revoke) {
            throw new ApiException(4003, "alert status does not allow send now");
        }
        a.setStatus(AlertStatus.sent);
        a.setSentTime(now);
        alertRepository.save(a);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alertId", a.getId());
        data.put("status", a.getStatus().name());
        data.put("sentTime", TimeUtils.toInstant(a.getSentTime()));
        return ApiResponse.ok("sent", data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> getForElder(long userId, long alertId) {
        ElderProfile elder = elderProfileRepository
                .findByClaimedUserId(userId)
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));
        EmergencyAlert a = alertRepository
                .findByIdAndElderProfileId(alertId, elder.getId())
                .orElseThrow(() -> new ApiException(4004, "alert not found"));
        LocalDateTime now = TimeUtils.now();
        maybeAutoSend(a, now);
        return ApiResponse.ok(toElderStatusBody(a, now));
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> listForChild(long childUserId, String statusParam, int page, int pageSize) {
        List<Long> elderIds = familyBindingRepository.findByChildUserIdAndStatus(childUserId, BindingStatus.active).stream()
                .map(FamilyBinding::getElderProfileId)
                .toList();
        if (elderIds.isEmpty()) {
            return ApiResponse.ok(toListPage(List.of(), page, pageSize, 0));
        }
        List<AlertStatus> statuses;
        if (statusParam != null && !statusParam.isBlank()) {
            if (!CHILD_LIST_STATUS_FILTER.contains(statusParam)) {
                throw new ApiException(4000, "invalid status filter");
            }
            statuses = List.of(AlertStatus.valueOf(statusParam));
        } else {
            statuses = List.of(AlertStatus.sent, AlertStatus.handled);
        }
        int p = Math.max(page, 1);
        int ps = Math.min(Math.max(pageSize, 1), 100);
        Page<EmergencyAlert> result = alertRepository.findByElderProfileIdInAndStatusIn(
                elderIds,
                statuses,
                PageRequest.of(p - 1, ps, Sort.by(Sort.Direction.DESC, "triggerTime")));
        Map<Long, String> names = elderProfileRepository.findAllById(
                        result.getContent().stream().map(EmergencyAlert::getElderProfileId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ElderProfile::getId, ElderProfile::getName));
        List<Map<String, Object>> list = new ArrayList<>();
        for (EmergencyAlert a : result.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("alertId", a.getId());
            row.put("elderId", a.getElderProfileId());
            row.put("elderName", names.getOrDefault(a.getElderProfileId(), ""));
            row.put("status", a.getStatus().name());
            row.put("triggerTime", TimeUtils.toInstant(a.getTriggerTime()));
            row.put("sentTime", TimeUtils.toInstant(a.getSentTime()));
            list.add(row);
        }
        return ApiResponse.ok(toListPage(list, p, ps, result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> detailForChild(long childUserId, long alertId) {
        Set<Long> bound = familyBindingRepository.findByChildUserIdAndStatus(childUserId, BindingStatus.active).stream()
                .map(FamilyBinding::getElderProfileId)
                .collect(Collectors.toSet());
        EmergencyAlert a = alertRepository.findById(alertId).orElseThrow(() -> new ApiException(4004, "alert not found"));
        if (!bound.contains(a.getElderProfileId())) {
            throw new ApiException(4005, "no permission to access this alert");
        }
        ElderProfile elder = elderProfileRepository
                .findById(a.getElderProfileId())
                .orElseThrow(() -> new ApiException(4004, "alert not found"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alertId", a.getId());
        data.put("elderId", a.getElderProfileId());
        data.put("elderName", elder.getName());
        data.put("status", a.getStatus().name());
        data.put("triggerTime", TimeUtils.toInstant(a.getTriggerTime()));
        data.put("sentTime", TimeUtils.toInstant(a.getSentTime()));
        data.put("location", null);
        data.put("remark", a.getRemark());
        return ApiResponse.ok(data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> handleForChild(long childUserId, long alertId, HandleEmergencyRequest req) {
        Set<Long> bound = familyBindingRepository.findByChildUserIdAndStatus(childUserId, BindingStatus.active).stream()
                .map(FamilyBinding::getElderProfileId)
                .collect(Collectors.toSet());
        EmergencyAlert a = alertRepository.findById(alertId).orElseThrow(() -> new ApiException(4004, "alert not found"));
        if (!bound.contains(a.getElderProfileId())) {
            throw new ApiException(4005, "no permission to access this alert");
        }
        if (a.getStatus() != AlertStatus.sent) {
            throw new ApiException(4006, "alert status does not allow handle");
        }
        LocalDateTime now = TimeUtils.now();
        String action = req.action().trim();
        if ("handled".equals(action)) {
            a.setStatus(AlertStatus.handled);
            a.setHandledTime(now);
            a.setHandledBy(childUserId);
            if (req.remark() != null && !req.remark().isBlank()) {
                a.setRemark(req.remark());
            }
            alertRepository.save(a);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("alertId", a.getId());
            data.put("status", a.getStatus().name());
            return ApiResponse.ok("handled", data);
        }
        if ("false_alarm".equals(action)) {
            a.setStatus(AlertStatus.false_alarm);
            a.setHandledTime(now);
            a.setHandledBy(childUserId);
            if (req.remark() != null && !req.remark().isBlank()) {
                a.setRemark(req.remark());
            }
            alertRepository.save(a);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("alertId", a.getId());
            data.put("status", a.getStatus().name());
            return ApiResponse.ok("false alarm marked", data);
        }
        throw new ApiException(4000, "invalid action");
    }

    @Transactional
    public int runAutoSendJob() {
        return alertRepository.bulkAutoSendPastDeadline(TimeUtils.now());
    }


    private void maybeAutoSend(EmergencyAlert a, LocalDateTime now) {
        if (a.getStatus() != AlertStatus.pending_revoke) {
            return;
        }
        if (a.getRevokeDeadline() == null || now.isBefore(a.getRevokeDeadline())) {
            return;
        }
        a.setStatus(AlertStatus.sent);
        a.setSentTime(now);
        alertRepository.save(a);
    }

    private Map<String, Object> toCreatedBody(EmergencyAlert a, LocalDateTime serverNow) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alertId", a.getId());
        m.put("status", a.getStatus().name());
        m.put("triggerTime", TimeUtils.toInstant(a.getTriggerTime()));
        m.put("revokeDeadline", TimeUtils.toInstant(a.getRevokeDeadline()));
        m.put("serverTime", TimeUtils.toInstant(serverNow));
        return m;
    }

    private Map<String, Object> toElderStatusBody(EmergencyAlert a, LocalDateTime serverNow) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alertId", a.getId());
        m.put("status", a.getStatus().name());
        m.put("triggerTime", TimeUtils.toInstant(a.getTriggerTime()));
        if (a.getStatus() == AlertStatus.pending_revoke) {
            m.put("revokeDeadline", TimeUtils.toInstant(a.getRevokeDeadline()));
        }
        if (a.getStatus() == AlertStatus.sent || a.getStatus() == AlertStatus.handled || a.getStatus() == AlertStatus.false_alarm) {
            m.put("sentTime", TimeUtils.toInstant(a.getSentTime()));
        }
        if (a.getStatus() == AlertStatus.cancelled) {
            m.put("cancelTime", TimeUtils.toInstant(a.getCancelTime()));
            if (a.getCancelMode() != null) {
                m.put("cancelMode", a.getCancelMode().name());
            }
        }
        m.put("serverTime", TimeUtils.toInstant(serverNow));
        return m;
    }

    private Map<String, Object> toListPage(List<Map<String, Object>> list, int page, int pageSize, long total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("list", list);
        m.put("page", page);
        m.put("pageSize", pageSize);
        m.put("total", total);
        return m;
    }
}
