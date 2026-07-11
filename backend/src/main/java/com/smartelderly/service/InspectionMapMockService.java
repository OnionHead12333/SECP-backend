package com.smartelderly.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.smartelderly.api.inspection.dto.ChildAlertDto;
import com.smartelderly.api.inspection.dto.FallEventRequest;
import com.smartelderly.api.inspection.dto.FallEventResponse;
import com.smartelderly.api.inspection.dto.InspectionMarkerDto;
import com.smartelderly.api.inspection.dto.InspectionPlaceDto;
import com.smartelderly.api.inspection.dto.InspectionRouteDto;
import com.smartelderly.api.inspection.dto.MapInfoDto;

@Service
public class InspectionMapMockService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AtomicLong markerId = new AtomicLong(7);
    private final AtomicLong fallEventId = new AtomicLong(1);
    private final AtomicLong alertId = new AtomicLong(1);
    private final List<InspectionMarkerDto> markers = new ArrayList<>();
    private final List<ChildAlertDto> childAlerts = new ArrayList<>();

    public InspectionMapMockService() {
        markers.add(new InspectionMarkerDto(
                1,
                "fall",
                120,
                240,
                "danger",
                "张爷爷疑似跌倒",
                "张爷爷疑似在走廊区域跌倒",
                "/static/mock/fall_001.jpg",
                "2026-07-10 15:30",
                "unhandled",
                "一层东侧走廊",
                1L,
                "张爷爷",
                "recent_identity",
                0.89,
                true));
        markers.add(new InspectionMarkerDto(
                2,
                "fall",
                360,
                260,
                "warning",
                "未知人员疑似跌倒",
                "检测到未知人员疑似跌倒，暂不通知子女端",
                "/static/mock/fall_unknown.jpg",
                "2026-07-10 15:34",
                "unhandled",
                "活动室入口",
                null,
                "未知人员",
                "unknown",
                0.0,
                false));
        markers.add(new InspectionMarkerDto(
                3,
                "crack",
                460,
                280,
                "warning",
                "地面裂缝",
                "巡检发现地面疑似裂缝",
                null,
                "2026-07-10 15:36",
                "unhandled",
                "南侧通道",
                null,
                null,
                null,
                null,
                null));
        markers.add(new InspectionMarkerDto(
                4,
                "robot",
                90,
                310,
                "info",
                "小车当前位置",
                "当前 mock 位置",
                null,
                "2026-07-10 15:37",
                "active",
                "充电桩附近",
                null,
                null,
                null,
                null,
                null));
        markers.add(new InspectionMarkerDto(
                5,
                "target",
                420,
                210,
                "info",
                "导航目标点",
                "预设目标：老人房间",
                null,
                "2026-07-10 15:38",
                "active",
                "老人房间",
                null,
                null,
                null,
                null,
                null));
        markers.add(new InspectionMarkerDto(
                6,
                "obstacle",
                280,
                180,
                "warning",
                "临时障碍物",
                "巡检路径上存在临时障碍物",
                null,
                "2026-07-10 15:39",
                "unhandled",
                "走廊中段",
                null,
                null,
                null,
                null,
                null));
        childAlerts.add(new ChildAlertDto(
                alertId.getAndIncrement(),
                "fall",
                1L,
                "张爷爷",
                "张爷爷疑似在走廊区域跌倒",
                "一层东侧走廊",
                "/static/mock/fall_001.jpg",
                "2026-07-10 15:30",
                "unhandled",
                true));
    }

    public MapInfoDto getMapInfo() {
        return new MapInfoDto(
                1,
                "养老院一层巡检地图",
                "/static/maps/yahboomcar.png",
                608,
                384,
                0.05,
                -10,
                -10,
                0,
                384);
    }

    public List<InspectionPlaceDto> getPlaces() {
        return List.of(
                new InspectionPlaceDto("home", "充电桩", 90, 310, "充电桩附近"),
                new InspectionPlaceDto("corridor_A", "一层东侧走廊", 120, 240, "一层东侧走廊"),
                new InspectionPlaceDto("elder_room", "老人房间A", 520, 300, "老人房间"),
                new InspectionPlaceDto("patrol_A", "巡检点A", 180, 210, "走廊西侧"),
                new InspectionPlaceDto("patrol_B", "巡检点B", 460, 280, "南侧通道"));
    }

    public List<InspectionRouteDto> getRoutes() {
        return List.of(new InspectionRouteDto(
                "route_1",
                "一层日常巡检路线",
                List.of("patrol_A", "patrol_B", "elder_room", "home")));
    }

    public synchronized List<InspectionMarkerDto> getMarkers() {
        return List.copyOf(markers);
    }

    public synchronized InspectionMarkerDto createMarker(InspectionMarkerDto request) {
        var marker = new InspectionMarkerDto(
                markerId.getAndIncrement(),
                defaultString(request.type(), "target"),
                request.x(),
                request.y(),
                defaultString(request.level(), "info"),
                defaultString(request.title(), "mock marker"),
                defaultString(request.message(), ""),
                request.imageUrl(),
                defaultString(request.time(), now()),
                defaultString(request.status(), "unhandled"),
                request.locationName(),
                request.elderId(),
                request.elderName(),
                request.identitySource(),
                request.identityConfidence(),
                request.notifiedChild());
        markers.add(marker);
        return marker;
    }

    public synchronized InspectionMarkerDto getMarker(long id) {
        return markers.stream()
                .filter(marker -> marker.id() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("marker not found: " + id));
    }

    public synchronized InspectionMarkerDto handleMarker(long id) {
        for (int i = 0; i < markers.size(); i++) {
            var marker = markers.get(i);
            if (marker.id() == id) {
                var handled = marker.handled();
                markers.set(i, handled);
                markChildAlertHandled(id);
                return handled;
            }
        }
        throw new NoSuchElementException("marker not found: " + id);
    }

    public synchronized FallEventResponse createFallEvent(FallEventRequest request) {
        if (!Boolean.TRUE.equals(request.fallAlert())) {
            return null;
        }

        long nextFallEventId = fallEventId.getAndIncrement();
        boolean notifiedChild = isKnownIdentity(request.identitySource());
        String elderName = request.elderName() == null || request.elderName().isBlank()
                ? "未知人员"
                : request.elderName();
        String level = mapRiskLevel(request.riskLevel());
        String time = defaultString(request.time(), now());
        String locationName = defaultString(request.locationName(), "未知区域");
        String message = elderName + "疑似在" + locationName + "跌倒";
        InspectionMarkerDto marker = createMarker(new InspectionMarkerDto(
                0,
                "fall",
                request.x() == null ? 0 : request.x(),
                request.y() == null ? 0 : request.y(),
                level,
                elderName + "疑似跌倒",
                message,
                request.imageUrl(),
                time,
                "unhandled",
                locationName,
                request.elderId(),
                elderName,
                defaultString(request.identitySource(), "unknown"),
                request.identityConfidence(),
                notifiedChild));

        long nextAlertId = 0;
        if (notifiedChild) {
            nextAlertId = alertId.getAndIncrement();
            childAlerts.add(new ChildAlertDto(
                    nextAlertId,
                    "fall",
                    request.elderId(),
                    elderName,
                    message,
                    locationName,
                    request.imageUrl(),
                    time,
                    "unhandled",
                    true));
        }

        return new FallEventResponse(nextFallEventId, marker.id(), nextAlertId, notifiedChild);
    }

    public synchronized List<ChildAlertDto> getChildAlerts() {
        return List.copyOf(childAlerts);
    }

    private void markChildAlertHandled(long markerId) {
        var marker = markers.stream()
                .filter(item -> item.id() == markerId)
                .findFirst()
                .orElse(null);
        if (marker == null || marker.elderId() == null) {
            return;
        }
        for (int i = 0; i < childAlerts.size(); i++) {
            var alert = childAlerts.get(i);
            if (marker.elderId().equals(alert.elderId()) && marker.time().equals(alert.time())) {
                childAlerts.set(i, new ChildAlertDto(
                        alert.id(),
                        alert.type(),
                        alert.elderId(),
                        alert.elderName(),
                        alert.message(),
                        alert.locationName(),
                        alert.imageUrl(),
                        alert.time(),
                        "handled",
                        alert.notified()));
            }
        }
    }

    private static boolean isKnownIdentity(String identitySource) {
        return "current_frame".equals(identitySource) || "recent_identity".equals(identitySource);
    }

    private static String mapRiskLevel(String riskLevel) {
        return switch (defaultString(riskLevel, "medium")) {
            case "low" -> "info";
            case "high" -> "danger";
            default -> "warning";
        };
    }

    private static String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
