package com.smartelderly.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.device.dto.DeviceHeartbeatRequest;
import com.smartelderly.api.device.dto.DeviceSosRequest;
import com.smartelderly.api.device.dto.DeviceSosResponse;
import com.smartelderly.api.device.dto.DeviceStatusResponse;
import com.smartelderly.domain.AlertStatus;
import com.smartelderly.domain.AlertType;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.EmergencyAlert;
import com.smartelderly.domain.EmergencyAlertRepository;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.FamilyMember;
import com.smartelderly.domain.FamilyMemberRepository;
import com.smartelderly.domain.IotDevice;
import com.smartelderly.domain.IotDeviceRepository;
import com.smartelderly.domain.TriggerMode;
import com.smartelderly.util.TimeUtils;

@Service
public class DeviceService {

    private static final String STATUS_ONLINE = "online";
    private static final String STATUS_OFFLINE = "offline";
    private static final String SOURCE_HARDWARE_DEVICE = "HARDWARE_DEVICE";
    private static final DateTimeFormatter SPACE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IotDeviceRepository deviceRepository;
    private final EmergencyAlertRepository alertRepository;
    private final FamilyBindingRepository familyBindingRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public DeviceService(
            IotDeviceRepository deviceRepository,
            EmergencyAlertRepository alertRepository,
            FamilyBindingRepository familyBindingRepository,
            FamilyMemberRepository familyMemberRepository,
            PasswordEncoder passwordEncoder) {
        this.deviceRepository = deviceRepository;
        this.alertRepository = alertRepository;
        this.familyBindingRepository = familyBindingRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DeviceStatusResponse heartbeat(DeviceHeartbeatRequest request) {
        IotDevice device = requireAuthorizedDevice(request.deviceId(), request.secret());
        LocalDateTime now = TimeUtils.now();
        device.setStatus(normalizeStatus(request.status()));
        device.setSignalStrength(request.signalStrength());
        device.setLastSeenAt(now);
        device.setLastHeartbeatAt(now);
        deviceRepository.save(device);
        return toStatusResponse(device);
    }

    @Transactional(readOnly = true)
    public List<DeviceStatusResponse> listForChildElder(long childUserId, long elderProfileId) {
        boolean bound = familyBindingRepository.existsByChildUserIdAndElderProfileIdAndStatus(
                childUserId, elderProfileId, BindingStatus.active);
        if (!bound) {
            throw new ApiException(4005, "no permission to access this elder devices");
        }

        List<Long> familyIds = familyMemberRepository.findByElderProfileIdOrderByIdAsc(elderProfileId).stream()
                .map(FamilyMember::getFamilyId)
                .distinct()
                .collect(Collectors.toList());
        if (familyIds.isEmpty()) {
            return List.of();
        }
        return deviceRepository.findByFamilyIdInOrderByUpdatedAtDesc(familyIds).stream()
                .map(DeviceService::toStatusResponse)
                .toList();
    }

    @Transactional
    public DeviceSosResponse createHardwareSos(DeviceSosRequest request) {
        if (!"HARDWARE_SOS".equalsIgnoreCase(request.eventType())) {
            throw new ApiException(4000, "invalid eventType");
        }
        IotDevice device = requireAuthorizedDevice(request.deviceId(), request.secret());
        LocalDateTime now = TimeUtils.now();
        LocalDateTime triggeredAt = parseClientTimeOrNow(request.triggeredAt(), now);
        String area = firstNonBlank(request.area(), device.getInstallArea());
        String message = firstNonBlank(request.message(), "家庭硬件 SOS 被触发");
        Long routeElderProfileId = resolveRouteElderProfileId(device);

        EmergencyAlert alert = new EmergencyAlert();
        // This elder id is only for routing via existing family_bindings.
        alert.setFamilyId(device.getFamilyId());
        alert.setElderProfileId(routeElderProfileId);
        alert.setAlertType(AlertType.sos);
        alert.setTriggerMode(TriggerMode.sensor);
        alert.setStatus(AlertStatus.sent);
        alert.setTriggerTime(triggeredAt);
        alert.setSentTime(now);
        alert.setSource(SOURCE_HARDWARE_DEVICE);
        alert.setDeviceId(device.getDeviceId());
        alert.setArea(area);
        alert.setHardwareMessage(message);
        alert.setRemark(message);
        alert.setCreatedAt(now);
        alertRepository.save(alert);

        device.setStatus(STATUS_ONLINE);
        device.setLastSeenAt(now);
        deviceRepository.save(device);

        return new DeviceSosResponse(true, alert.getId(), "hardware sos created");
    }

    private IotDevice requireAuthorizedDevice(String deviceId, String secret) {
        IotDevice device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ApiException(4004, "device not found"));
        if (!passwordEncoder.matches(secret, device.getSecretHash())) {
            throw new ApiException(4010, "device auth failed");
        }
        return device;
    }

    private Long resolveRouteElderProfileId(IotDevice device) {
        if (device.getFamilyId() == null) {
            throw new ApiException(4000, "device family missing");
        }
        if (device.getRouteElderProfileId() != null) {
            return device.getRouteElderProfileId();
        }
        return familyMemberRepository
                .findFirstByFamilyIdAndPrimaryTrueOrderByIdAsc(device.getFamilyId())
                .map(FamilyMember::getElderProfileId)
                .orElseThrow(() -> new ApiException(4000, "route elder missing"));
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_ONLINE;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (STATUS_ONLINE.equals(normalized) || STATUS_OFFLINE.equals(normalized)) {
            return normalized;
        }
        throw new ApiException(4000, "invalid device status");
    }

    private static LocalDateTime parseClientTimeOrNow(String raw, LocalDateTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.trim();
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, SPACE_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new ApiException(4000, "invalid triggeredAt");
            }
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private static DeviceStatusResponse toStatusResponse(IotDevice device) {
        return new DeviceStatusResponse(
                device.getDeviceId(),
                device.getFamilyId(),
                device.getStatus(),
                device.getInstallArea(),
                TimeUtils.toInstant(device.getLastHeartbeatAt()),
                TimeUtils.toInstant(device.getLastSeenAt()),
                device.getSignalStrength());
    }
}
