package com.smartelderly.api.location;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.LocationGuardErrorRequest;
import com.smartelderly.api.location.dto.LocationGuardPermissionRequest;
import com.smartelderly.api.location.dto.LocationGuardResponse;
import com.smartelderly.api.location.dto.StartLocationGuardRequest;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.location.ElderLocationGuardService;

@RestController
@RequestMapping("/v1/elder/location-guard")
public class ElderLocationGuardController {

    private final ElderLocationGuardService elderLocationGuardService;

    public ElderLocationGuardController(ElderLocationGuardService elderLocationGuardService) {
        this.elderLocationGuardService = elderLocationGuardService;
    }

    @GetMapping
    public ApiResponse<LocationGuardResponse> getGuardSetting() {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.ok(elderLocationGuardService.getForElderUser(user.userId()));
    }

    @PostMapping("/start")
    public ApiResponse<LocationGuardResponse> startGuard(@RequestBody StartLocationGuardRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.ok("started", elderLocationGuardService.start(user.userId(), request));
    }

    @PostMapping("/stop")
    public ApiResponse<LocationGuardResponse> stopGuard() {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.ok("stopped", elderLocationGuardService.stop(user.userId()));
    }

    @PutMapping("/permissions")
    public ApiResponse<LocationGuardResponse> syncPermissions(@RequestBody LocationGuardPermissionRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.ok("saved", elderLocationGuardService.syncPermissions(user.userId(), request));
    }

    @PostMapping("/error")
    public ApiResponse<LocationGuardResponse> reportError(@RequestBody LocationGuardErrorRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.ok("saved", elderLocationGuardService.reportError(user.userId(), request));
    }
}
