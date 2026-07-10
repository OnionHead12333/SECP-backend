package com.smartelderly.api.location;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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

@RestController
@RequestMapping("/v1/elder")
public class ElderGeofenceController {

    private final GeofenceService geofenceService;

    public ElderGeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @GetMapping("/home-geofence")
    public ApiResponse<List<HomeGeofenceResponse>> getHomeGeofence() {
        var user = SecurityUtils.requireRole(UserRole.elder);
        long elderId = geofenceService.resolveElderProfileIdForElderUser(user.userId());
        return geofenceService.getHomeGeofenceOrEmpty(elderId);
    }

    @PutMapping("/home-geofence")
    public ApiResponse<SaveHomeGeofenceResponse> saveHomeGeofence(
            @Valid @RequestBody SaveHomeGeofenceRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        long elderId = geofenceService.resolveElderProfileIdForElderUser(user.userId());
        return geofenceService.saveHomeGeofence(elderId, request);
    }
}
