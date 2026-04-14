package com.smartelderly.api;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.HandleEmergencyRequest;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.EmergencyAlertService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/child/emergency-alerts")
@Validated
public class ChildEmergencyAlertController {

    private final EmergencyAlertService emergencyAlertService;

    public ChildEmergencyAlertController(EmergencyAlertService emergencyAlertService) {
        this.emergencyAlertService = emergencyAlertService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        var user = SecurityUtils.requireRole(UserRole.child);
        return emergencyAlertService.listForChild(user.userId(), status, page, pageSize);
    }

    @GetMapping("/{alertId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("alertId") long alertId) {
        var user = SecurityUtils.requireRole(UserRole.child);
        return emergencyAlertService.detailForChild(user.userId(), alertId);
    }

    @PostMapping("/{alertId}/handle")
    public ApiResponse<Map<String, Object>> handle(
            @PathVariable("alertId") long alertId,
            @Valid @RequestBody HandleEmergencyRequest request) {
        var user = SecurityUtils.requireRole(UserRole.child);
        return emergencyAlertService.handleForChild(user.userId(), alertId, request);
    }
}
