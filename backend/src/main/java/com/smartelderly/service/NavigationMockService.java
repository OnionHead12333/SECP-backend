package com.smartelderly.service;

import org.springframework.stereotype.Service;

import com.smartelderly.api.inspection.dto.NavigationStartRequest;
import com.smartelderly.api.inspection.dto.NavigationStatusDto;
import com.smartelderly.api.inspection.dto.NavigationStatusUpdateRequest;
import com.smartelderly.api.inspection.dto.ObstacleStatusDto;

@Service
public class NavigationMockService {

    private NavigationStatusDto navigationStatus = new NavigationStatusDto(
            "idle",
            260,
            300,
            520,
            300,
            null,
            "未开始导航");
    private ObstacleStatusDto obstacleStatus = new ObstacleStatusDto(
            "safe",
            null,
            null,
            "当前无障碍物");

    public synchronized NavigationStatusDto getNavigationStatus() {
        return navigationStatus;
    }

    public synchronized NavigationStatusDto startNavigation(NavigationStartRequest request) {
        int targetX = request.targetX() == null ? 520 : request.targetX();
        int targetY = request.targetY() == null ? 300 : request.targetY();
        String targetName = request.targetName() == null || request.targetName().isBlank()
                ? "老人房间A"
                : request.targetName();
        navigationStatus = new NavigationStatusDto(
                "running",
                navigationStatus.currentX(),
                navigationStatus.currentY(),
                targetX,
                targetY,
                targetName,
                "正在导航到" + targetName);
        return navigationStatus;
    }

    public synchronized NavigationStatusDto cancelNavigation() {
        navigationStatus = new NavigationStatusDto(
                "idle",
                navigationStatus.currentX(),
                navigationStatus.currentY(),
                navigationStatus.targetX(),
                navigationStatus.targetY(),
                navigationStatus.targetName(),
                "导航已取消");
        return navigationStatus;
    }

    public synchronized NavigationStatusDto returnHome() {
        navigationStatus = new NavigationStatusDto(
                "running",
                navigationStatus.currentX(),
                navigationStatus.currentY(),
                90,
                310,
                "home",
                "正在一键返航");
        return navigationStatus;
    }

    public synchronized NavigationStatusDto updateNavigationStatus(NavigationStatusUpdateRequest request) {
        navigationStatus = new NavigationStatusDto(
                defaultString(request.navigationStatus(), navigationStatus.navigationStatus()),
                request.currentX() == null ? navigationStatus.currentX() : request.currentX(),
                request.currentY() == null ? navigationStatus.currentY() : request.currentY(),
                request.targetX() == null ? navigationStatus.targetX() : request.targetX(),
                request.targetY() == null ? navigationStatus.targetY() : request.targetY(),
                defaultString(request.targetName(), navigationStatus.targetName()),
                defaultString(request.message(), navigationStatus.message()));
        return navigationStatus;
    }

    public synchronized ObstacleStatusDto getObstacleStatus() {
        return obstacleStatus;
    }

    public synchronized ObstacleStatusDto updateObstacleStatus(ObstacleStatusDto request) {
        obstacleStatus = new ObstacleStatusDto(
                defaultString(request.obstacleStatus(), obstacleStatus.obstacleStatus()),
                request.x(),
                request.y(),
                defaultString(request.message(), obstacleStatus.message()));
        return obstacleStatus;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
