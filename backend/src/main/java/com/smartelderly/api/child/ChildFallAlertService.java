package com.smartelderly.api.child;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.inspection.RobotMapMarker;
import com.smartelderly.api.inspection.RobotMapMarkerRepository;

@Service
public class ChildFallAlertService {

    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter HANDLE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RobotMapMarkerRepository markerRepository;

    public ChildFallAlertService(RobotMapMarkerRepository markerRepository) {
        this.markerRepository = markerRepository;
    }

    @Transactional(readOnly = true)
    public List<ChildFallAlert> listFallAlerts(Long childUserId) {
        return markerRepository.findFallAlertsForChild(childUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ChildFallAlert> getFallAlert(long id, Long childUserId) {
        return markerRepository.findFallAlertByIdAndChildUserId(id, childUserId).map(this::toResponse);
    }

    private ChildFallAlert toResponse(RobotMapMarker marker) {
        String description = marker.getDescription();
        return new ChildFallAlert(
                marker.getId(),
                marker.getMarkerType(),
                marker.getTitle(),
                description,
                description,
                marker.getElderProfileId(),
                marker.getElderName(),
                marker.getIdentitySource(),
                marker.getIdentityConfidence(),
                marker.getNotifiedChild(),
                marker.getLocationName(),
                marker.getLocationX(),
                marker.getLocationY(),
                marker.getLevel(),
                marker.getStatus(),
                marker.getImageUrl(),
                formatEventTime(firstPresent(marker.getEventTime(), marker.getCreatedAt())),
                marker.getHandledByName(),
                marker.getHandleRemark(),
                formatHandleTime(marker.getHandledAt()));
    }

    private static LocalDateTime firstPresent(LocalDateTime preferred, LocalDateTime fallback) {
        return preferred == null ? fallback : preferred;
    }

    private static String formatEventTime(LocalDateTime value) {
        return value == null ? null : value.format(EVENT_TIME_FORMATTER);
    }

    private static String formatHandleTime(LocalDateTime value) {
        return value == null ? null : value.format(HANDLE_TIME_FORMATTER);
    }
}
