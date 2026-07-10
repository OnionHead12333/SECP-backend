package com.smartelderly.service.location;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.LocationPermissionRequest;
import com.smartelderly.api.location.dto.LocationPermissionResponse;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;

@Service
public class LocationPermissionService {

    private final ElderProfileRepository elderProfileRepository;

    public LocationPermissionService(ElderProfileRepository elderProfileRepository) {
        this.elderProfileRepository = elderProfileRepository;
    }

    /**
     * 获取老人的定位权限状态
     * @param claimedUserId 当前登录老人用户ID
     * @return ApiResponse
     */
    public ApiResponse<LocationPermissionResponse> getLocationPermission(Long claimedUserId) {
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(claimedUserId)
                .orElseThrow(() -> new ApiException(4002, "elder profile not found"));

        LocationPermissionResponse response = new LocationPermissionResponse(
                elder.getLocationPermissionForeground(),
                elder.getLocationPermissionBackground(),
                elder.getPermissionUpdatedAt()
        );

        return ApiResponse.ok(response);
    }

    /**
     * 更新老人的定位权限状态
     * 根据当前登录老人用户ID，更新对应档案的权限快照
     * @param claimedUserId 当前登录老人用户ID
     * @param request 权限更新请求
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<LocationPermissionResponse> updateLocationPermission(
            Long claimedUserId, LocationPermissionRequest request) {
        
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(claimedUserId)
                .orElseThrow(() -> new ApiException(4002, "elder profile not found"));

        // 更新权限字段
        elder.setLocationPermissionForeground(request.getForegroundGranted());
        elder.setLocationPermissionBackground(request.getBackgroundGranted());

        // 如果请求中未提供时间，则使用当前时间
        if (request.getPermissionUpdatedAt() != null) {
            elder.setPermissionUpdatedAt(request.getPermissionUpdatedAt());
        } else {
            elder.setPermissionUpdatedAt(LocalDateTime.now());
        }

        elderProfileRepository.save(elder);

        // 返回更新后的权限状态
        LocationPermissionResponse response = new LocationPermissionResponse(
                elder.getLocationPermissionForeground(),
                elder.getLocationPermissionBackground(),
                elder.getPermissionUpdatedAt()
        );

        return ApiResponse.ok("saved", response);
    }
}
