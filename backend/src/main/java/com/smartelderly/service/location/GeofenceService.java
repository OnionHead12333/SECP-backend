package com.smartelderly.service.location;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.HomeGeofenceResponse;
import com.smartelderly.api.location.dto.SaveHomeGeofenceRequest;
import com.smartelderly.api.location.dto.SaveHomeGeofenceResponse;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.location.Geofence;
import com.smartelderly.domain.location.GeofenceRepository;

@Service
public class GeofenceService {

    private static final String HOME_GEOFENCE_NAME = "家";
    public static final int DEFAULT_HOME_RADIUS_METERS = 500;

    private final GeofenceRepository geofenceRepository;
    private final ElderProfileRepository elderProfileRepository;

    public GeofenceService(GeofenceRepository geofenceRepository,
            ElderProfileRepository elderProfileRepository) {
        this.geofenceRepository = geofenceRepository;
        this.elderProfileRepository = elderProfileRepository;
    }

    /**
     * 获取老人的全部围栏信息
     * @param elderId 老人ID
     * @return ApiResponse 包含围栏列表
     */
    public ApiResponse<List<HomeGeofenceResponse>> getHomeGeofence(Long elderId) {
        ensureElderExists(elderId);
        var geofences = geofenceRepository.findByElderProfileId(elderId);
        if (geofences.isEmpty()) {
            throw new ApiException(4042, "home geofence not found");
        }
        return ApiResponse.ok(toResponseList(geofences));
    }

    /**
     * 获取家围栏；不存在时返回空列表（老人端查询用）。
     */
    public ApiResponse<List<HomeGeofenceResponse>> getHomeGeofenceOrEmpty(Long elderId) {
        ensureElderExists(elderId);
        var geofences = geofenceRepository.findByElderProfileId(elderId);
        return ApiResponse.ok(toResponseList(geofences));
    }

    public boolean hasHomeGeofence(Long elderId) {
        return geofenceRepository.findByElderProfileIdAndName(elderId, HOME_GEOFENCE_NAME).isPresent();
    }

    public long resolveElderProfileIdForElderUser(Long userId) {
        return elderProfileRepository.findByClaimedUserId(userId)
                .orElseThrow(() -> new ApiException(4030, "请先完成老人档案认领后再设置家的位置"))
                .getId();
    }

    /**
     * 保存老人的家围栏
     * 如果围栏不存在则创建，如果已存在则更新
     * @param elderId 老人ID
     * @param request 保存请求
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<SaveHomeGeofenceResponse> saveHomeGeofence(Long elderId, SaveHomeGeofenceRequest request) {
        ensureElderExists(elderId);
        normalizeSaveRequest(request);

        var existingGeofence = geofenceRepository.findByElderProfileIdAndName(elderId, request.getName());

        Geofence geofence;
        if (existingGeofence.isPresent()) {
            // 更新现有围栏
            geofence = existingGeofence.get();
            geofence.setCenterLatitude(request.getCenterLatitude());
            geofence.setCenterLongitude(request.getCenterLongitude());
            geofence.setRadius(request.getRadius());
            geofence.setIsEnabled(request.getEnabled());
            geofence.setUpdatedAt(LocalDateTime.now());
        } else {
            // 创建新围栏
            geofence = new Geofence();
            geofence.setElderProfileId(elderId);
            geofence.setName(request.getName());
            geofence.setCenterLatitude(request.getCenterLatitude());
            geofence.setCenterLongitude(request.getCenterLongitude());
            geofence.setRadius(request.getRadius());
            geofence.setIsEnabled(request.getEnabled());
            geofence.setCreatedAt(LocalDateTime.now());
            geofence.setUpdatedAt(LocalDateTime.now());
        }

        // 保存或更新围栏
        geofenceRepository.save(geofence);

        // 返回成功响应
        SaveHomeGeofenceResponse response = new SaveHomeGeofenceResponse(elderId);
        return ApiResponse.ok("saved", response);
    }

    private void ensureElderExists(Long elderId) {
        elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));
    }

    private void normalizeSaveRequest(SaveHomeGeofenceRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName(HOME_GEOFENCE_NAME);
        }
        if (request.getRadius() == null || request.getRadius() <= 0) {
            request.setRadius(DEFAULT_HOME_RADIUS_METERS);
        }
        if (request.getEnabled() == null) {
            request.setEnabled(true);
        }
    }

    private List<HomeGeofenceResponse> toResponseList(List<Geofence> geofences) {
        return geofences.stream()
                .map(geofence -> new HomeGeofenceResponse(
                        geofence.getName(),
                        geofence.getCenterLatitude(),
                        geofence.getCenterLongitude(),
                        geofence.getRadius(),
                        geofence.getIsEnabled()))
                .collect(Collectors.toList());
    }
}
