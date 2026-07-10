package com.smartelderly.api.location;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.HomeGeofenceResponse;
import com.smartelderly.api.location.dto.SaveHomeGeofenceRequest;
import com.smartelderly.api.location.dto.SaveHomeGeofenceResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.location.GeofenceService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/child/elders")
public class ChildGeofenceController {

    private final GeofenceService geofenceService;

    public ChildGeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    /**
     * 获取家围栏列表
     * @param elderId 老人ID
     * @return 家围栏列表
     */
    @GetMapping("/{elderId}/home-geofence")
    public ApiResponse<List<HomeGeofenceResponse>> getHomeGeofence(@PathVariable("elderId") Long elderId) {
        var user = SecurityUtils.requireRole(UserRole.child);
        return geofenceService.getHomeGeofence(elderId);
    }

    /**
     * 保存家围栏
     * @param elderId 老人ID
     * @param request 保存请求
     * @return 保存结果
     */
    @PutMapping("/{elderId}/home-geofence")
    public ApiResponse<SaveHomeGeofenceResponse> saveHomeGeofence(
            @PathVariable("elderId") Long elderId,
            @Valid @RequestBody SaveHomeGeofenceRequest request) {
        var user = SecurityUtils.requireRole(UserRole.child);
        return geofenceService.saveHomeGeofence(elderId, request);
    }
}
