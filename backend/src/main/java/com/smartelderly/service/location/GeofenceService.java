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
        // 验证老人是否存在
        var elder = elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 查询该老人下的所有围栏
        var geofences = geofenceRepository.findByElderProfileId(elderId);
        if (geofences.isEmpty()) {
            throw new ApiException(4042, "home geofence not found");
        }

        // 转换为响应对象列表
        List<HomeGeofenceResponse> responseList = geofences.stream()
                .map(geofence -> new HomeGeofenceResponse(
                        geofence.getName(),
                        geofence.getCenterLatitude(),
                        geofence.getCenterLongitude(),
                        geofence.getRadius(),
                        geofence.getIsEnabled()
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(responseList);
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
        // 验证老人是否存在
        var elder = elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 查询是否已存在同名围栏
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
}
