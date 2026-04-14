package com.smartelderly.api;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.CreateEmergencyRequest;
import com.smartelderly.api.dto.RevokeRequest;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.EmergencyAlertService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/elder/emergency-alerts")
@Validated
public class ElderEmergencyAlertController {

    private final EmergencyAlertService emergencyAlertService;

    public ElderEmergencyAlertController(EmergencyAlertService emergencyAlertService) {
        this.emergencyAlertService = emergencyAlertService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateEmergencyRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return emergencyAlertService.createForElder(user.userId(), request);
    }

    @PostMapping("/{alertId}/revoke")
    public ApiResponse<Map<String, Object>> revoke(
            @PathVariable("alertId") long alertId,
            @Valid @RequestBody RevokeRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return emergencyAlertService.revokeForElder(user.userId(), alertId, request);
    }

    @PostMapping("/{alertId}/send-now")
    public ApiResponse<Map<String, Object>> sendNow(@PathVariable("alertId") long alertId) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return emergencyAlertService.sendNowForElder(user.userId(), alertId);
    }

    @GetMapping("/{alertId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable("alertId") long alertId) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return emergencyAlertService.getForElder(user.userId(), alertId);
    }
}
