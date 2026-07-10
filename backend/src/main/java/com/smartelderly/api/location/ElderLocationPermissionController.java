package com.smartelderly.api.location;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.LocationPermissionRequest;
import com.smartelderly.api.location.dto.LocationPermissionResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.location.LocationPermissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/elder/location-permissions")
public class ElderLocationPermissionController {

    private final LocationPermissionService locationPermissionService;

    public ElderLocationPermissionController(LocationPermissionService locationPermissionService) {
        this.locationPermissionService = locationPermissionService;
    }

    /**
     * 获取定位权限状态
     * 老人端查询后端当前记录的定位权限快照
     * @return 定位权限信息
     */
    @GetMapping
    public ApiResponse<LocationPermissionResponse> getLocationPermission() {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return locationPermissionService.getLocationPermission(user.userId());
    }

    /**
     * 更新定位权限状态
     * 老人端将当前定位权限状态同步给后端
     * @param request 权限更新请求
     * @return 更新后的权限信息
     */
    @PutMapping
    public ApiResponse<LocationPermissionResponse> updateLocationPermission(
            @Valid @RequestBody LocationPermissionRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return locationPermissionService.updateLocationPermission(user.userId(), request);
    }
}
