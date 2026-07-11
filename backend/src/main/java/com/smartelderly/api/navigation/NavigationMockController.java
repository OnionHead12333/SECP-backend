package com.smartelderly.api.navigation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.dto.ApiResponse;
import com.smartelderly.api.inspection.dto.NavigationStartRequest;
import com.smartelderly.api.inspection.dto.NavigationStatusDto;
import com.smartelderly.api.inspection.dto.NavigationStatusUpdateRequest;
import com.smartelderly.api.inspection.dto.ObstacleStatusDto;
import com.smartelderly.service.NavigationMockService;

@RestController
@RequestMapping("/v1")
public class NavigationMockController {

    private final NavigationMockService navigationMockService;

    public NavigationMockController(NavigationMockService navigationMockService) {
        this.navigationMockService = navigationMockService;
    }

    @GetMapping("/navigation/status")
    public ApiResponse<NavigationStatusDto> navigationStatus() {
        return ApiResponse.ok(navigationMockService.getNavigationStatus());
    }

    @PostMapping("/navigation/start")
    public ApiResponse<NavigationStatusDto> startNavigation(@RequestBody NavigationStartRequest request) {
        return ApiResponse.ok("navigation started", navigationMockService.startNavigation(request));
    }

    @PostMapping("/navigation/cancel")
    public ApiResponse<NavigationStatusDto> cancelNavigation() {
        return ApiResponse.ok("navigation canceled", navigationMockService.cancelNavigation());
    }

    @PostMapping("/navigation/return-home")
    public ApiResponse<NavigationStatusDto> returnHome() {
        return ApiResponse.ok("return home started", navigationMockService.returnHome());
    }

    @PostMapping("/navigation/status")
    public ApiResponse<NavigationStatusDto> updateNavigationStatus(
            @RequestBody NavigationStatusUpdateRequest request) {
        return ApiResponse.ok("navigation status updated", navigationMockService.updateNavigationStatus(request));
    }

    @GetMapping("/obstacle/status")
    public ApiResponse<ObstacleStatusDto> obstacleStatus() {
        return ApiResponse.ok(navigationMockService.getObstacleStatus());
    }

    @PostMapping("/obstacle/status")
    public ApiResponse<ObstacleStatusDto> updateObstacleStatus(@RequestBody ObstacleStatusDto request) {
        return ApiResponse.ok("obstacle status updated", navigationMockService.updateObstacleStatus(request));
    }
}
