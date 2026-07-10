package com.smartelderly.service.location;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.location.dto.LocationGuardErrorRequest;
import com.smartelderly.api.location.dto.LocationGuardPermissionRequest;
import com.smartelderly.api.location.dto.LocationGuardResponse;
import com.smartelderly.api.location.dto.StartLocationGuardRequest;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.location.ElderLocationGuardSetting;
import com.smartelderly.domain.location.ElderLocationGuardSettingRepository;

@Service
public class ElderLocationGuardService {

    private static final Set<String> ALLOWED_MODES = Set.of("off", "foreground", "background");

    private final ElderProfileRepository elderProfileRepository;
    private final ElderLocationGuardSettingRepository guardSettingRepository;

    public ElderLocationGuardService(ElderProfileRepository elderProfileRepository,
                                     ElderLocationGuardSettingRepository guardSettingRepository) {
        this.elderProfileRepository = elderProfileRepository;
        this.guardSettingRepository = guardSettingRepository;
    }

    @Transactional
    public LocationGuardResponse getForElderUser(Long userId) {
        ElderProfile elder = getElderProfile(userId);
        return LocationGuardResponse.from(getOrCreate(elder));
    }

    @Transactional
    public LocationGuardResponse start(Long userId, StartLocationGuardRequest request) {
        ElderProfile elder = getElderProfile(userId);
        ElderLocationGuardSetting setting = getOrCreate(elder);
        LocalDateTime now = LocalDateTime.now();

        String mode = normalizeMode(request.getMode(), "background");
        setting.setEnabled(true);
        setting.setMode(mode);
        setting.setIntervalSeconds(defaultInt(request.getIntervalSeconds(), setting.getIntervalSeconds(), 600));
        setting.setOutsideIntervalSeconds(defaultInt(request.getOutsideIntervalSeconds(), setting.getOutsideIntervalSeconds(), 300));
        setting.setBackgroundRequired(defaultBool(request.getBackgroundRequired(), setting.getBackgroundRequired(), true));
        applyPermissionSnapshot(elder, setting, request.getForegroundGranted(), request.getBackgroundGranted(), request.getBatteryOptimizationIgnored(), now);
        setting.setLastStartedAt(now);
        setting.setLastStoppedAt(null);
        setting.setLastError(null);
        setting.setUpdatedBy(userId);
        setting.setUpdatedAt(now);

        elderProfileRepository.save(elder);
        return LocationGuardResponse.from(guardSettingRepository.save(setting));
    }

    @Transactional
    public LocationGuardResponse stop(Long userId) {
        ElderProfile elder = getElderProfile(userId);
        ElderLocationGuardSetting setting = getOrCreate(elder);
        LocalDateTime now = LocalDateTime.now();

        setting.setEnabled(false);
        setting.setMode("off");
        setting.setLastStoppedAt(now);
        setting.setUpdatedBy(userId);
        setting.setUpdatedAt(now);

        return LocationGuardResponse.from(guardSettingRepository.save(setting));
    }

    @Transactional
    public LocationGuardResponse syncPermissions(Long userId, LocationGuardPermissionRequest request) {
        ElderProfile elder = getElderProfile(userId);
        ElderLocationGuardSetting setting = getOrCreate(elder);
        LocalDateTime now = LocalDateTime.now();

        applyPermissionSnapshot(elder, setting, request.getForegroundGranted(), request.getBackgroundGranted(), request.getBatteryOptimizationIgnored(), now);
        if (Boolean.TRUE.equals(setting.getEnabled()) && Boolean.TRUE.equals(setting.getBackgroundRequired())
                && !Boolean.TRUE.equals(setting.getBackgroundGranted())) {
            setting.setLastError("后台定位权限未开启");
        }
        setting.setUpdatedBy(userId);
        setting.setUpdatedAt(now);

        elderProfileRepository.save(elder);
        return LocationGuardResponse.from(guardSettingRepository.save(setting));
    }

    @Transactional
    public LocationGuardResponse reportError(Long userId, LocationGuardErrorRequest request) {
        ElderProfile elder = getElderProfile(userId);
        ElderLocationGuardSetting setting = getOrCreate(elder);
        LocalDateTime now = LocalDateTime.now();

        setting.setLastError(limitError(request.getMessage()));
        setting.setUpdatedBy(userId);
        setting.setUpdatedAt(now);

        return LocationGuardResponse.from(guardSettingRepository.save(setting));
    }

    @Transactional
    public void markUploadSuccess(Long elderProfileId, LocalDateTime uploadTime) {
        ElderLocationGuardSetting setting = guardSettingRepository.findByElderProfileId(elderProfileId)
                .orElseGet(() -> createDefault(elderProfileId, null));
        setting.setLastUploadAt(uploadTime != null ? uploadTime : LocalDateTime.now());
        setting.setLastError(null);
        setting.setUpdatedAt(LocalDateTime.now());
        guardSettingRepository.save(setting);
    }

    @Transactional
    public void markUploadFailure(Long elderProfileId, String message) {
        ElderLocationGuardSetting setting = guardSettingRepository.findByElderProfileId(elderProfileId)
                .orElseGet(() -> createDefault(elderProfileId, null));
        setting.setLastError(limitError(message));
        setting.setUpdatedAt(LocalDateTime.now());
        guardSettingRepository.save(setting);
    }

    private ElderProfile getElderProfile(Long userId) {
        return elderProfileRepository.findByClaimedUserId(userId)
                .orElseThrow(() -> new ApiException(4002, "elder profile not found"));
    }

    private ElderLocationGuardSetting getOrCreate(ElderProfile elder) {
        return guardSettingRepository.findByElderProfileId(elder.getId())
                .orElseGet(() -> guardSettingRepository.save(createDefault(elder.getId(), elder)));
    }

    private ElderLocationGuardSetting createDefault(Long elderProfileId, ElderProfile elder) {
        ElderLocationGuardSetting setting = new ElderLocationGuardSetting();
        setting.setElderProfileId(elderProfileId);
        setting.setEnabled(false);
        setting.setMode("off");
        setting.setIntervalSeconds(600);
        setting.setOutsideIntervalSeconds(300);
        setting.setBackgroundRequired(true);
        setting.setForegroundGranted(elder != null && Boolean.TRUE.equals(elder.getLocationPermissionForeground()));
        setting.setBackgroundGranted(elder != null && Boolean.TRUE.equals(elder.getLocationPermissionBackground()));
        setting.setBatteryOptimizationIgnored(false);
        LocalDateTime now = LocalDateTime.now();
        setting.setCreatedAt(now);
        setting.setUpdatedAt(now);
        return setting;
    }

    private void applyPermissionSnapshot(ElderProfile elder, ElderLocationGuardSetting setting,
                                         Boolean foregroundGranted, Boolean backgroundGranted,
                                         Boolean batteryOptimizationIgnored, LocalDateTime now) {
        if (foregroundGranted != null) {
            setting.setForegroundGranted(foregroundGranted);
            elder.setLocationPermissionForeground(foregroundGranted);
        }
        if (backgroundGranted != null) {
            setting.setBackgroundGranted(backgroundGranted);
            elder.setLocationPermissionBackground(backgroundGranted);
        }
        if (batteryOptimizationIgnored != null) {
            setting.setBatteryOptimizationIgnored(batteryOptimizationIgnored);
        }
        if (foregroundGranted != null || backgroundGranted != null) {
            elder.setPermissionUpdatedAt(now);
        }
    }

    private String normalizeMode(String mode, String defaultMode) {
        String value = mode == null || mode.isBlank() ? defaultMode : mode.trim();
        if (!ALLOWED_MODES.contains(value)) {
            throw new ApiException(4000, "invalid location guard mode");
        }
        return value;
    }

    private Integer defaultInt(Integer requested, Integer current, Integer fallback) {
        if (requested != null && requested > 0) return requested;
        if (current != null && current > 0) return current;
        return fallback;
    }

    private Boolean defaultBool(Boolean requested, Boolean current, Boolean fallback) {
        if (requested != null) return requested;
        if (current != null) return current;
        return fallback;
    }

    private String limitError(String message) {
        if (message == null || message.isBlank()) {
            return "定位上传失败";
        }
        String trimmed = message.trim();
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }
}
