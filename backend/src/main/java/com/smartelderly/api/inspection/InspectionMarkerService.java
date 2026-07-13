package com.smartelderly.api.inspection;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InspectionMarkerService {

    private static final long DEFAULT_MAP_ID = 1L;
    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter HANDLE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<DateTimeFormatter> INPUT_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

    private final RobotMapMarkerRepository markerRepository;

    public InspectionMarkerService(RobotMapMarkerRepository markerRepository) {
        this.markerRepository = markerRepository;
    }

    public InspectionMapInfo getMapInfo() {
        return new InspectionMapInfo(
                "floor1",
                "\u517b\u8001\u9662\u4e00\u5c42",
                "assets/robot_maps/yahboomcar.png",
                608,
                384,
                0.05,
                -10.0,
                -10.0,
                384);
    }

    @Transactional(readOnly = true)
    public List<InspectionMarker> listMarkers() {
        return markerRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<InspectionMarker> getMarker(long id) {
        return markerRepository.findById(id).map(this::toResponse);
    }

    @Transactional
    public InspectionMarker createMarker(InspectionMarker request, long createdByUserId) {
        RobotMapMarker marker = toEntity(request);
        marker.setCreatedBy(createdByUserId);
        return toResponse(markerRepository.save(marker));
    }

    @Transactional
    public Optional<InspectionMarker> handleMarker(
            long id,
            InspectionHandleRequest request,
            long handledByUserId) {
        return markerRepository.findById(id)
                .map(marker -> {
                    marker.setStatus("handled");
                    marker.setHandledBy(handledByUserId);
                    marker.setHandledByName(request.handler());
                    marker.setHandleRemark(request.remark());
                    marker.setHandledAt(LocalDateTime.now());
                    return toResponse(markerRepository.save(marker));
                });
    }

    private RobotMapMarker toEntity(InspectionMarker request) {
        RobotMapMarker marker = new RobotMapMarker();
        marker.setMapId(request.getMapId() == null ? DEFAULT_MAP_ID : request.getMapId());
        marker.setElderProfileId(request.getElderId());
        marker.setMarkerType(request.getType());
        marker.setLocationX(request.getX());
        marker.setLocationY(request.getY());
        marker.setLocationName(request.getLocationName());
        marker.setTitle(request.getTitle());
        marker.setDescription(firstPresent(request.getDescription(), request.getMessage()));
        marker.setLevel(request.getLevel());
        marker.setStatus(request.getStatus());
        marker.setSource(request.getSource());
        marker.setImageUrl(request.getImageUrl());
        marker.setEventTime(parseEventTime(request.getTime()));
        marker.setElderName(request.getElderName());
        marker.setIdentitySource(request.getIdentitySource());
        marker.setIdentityConfidence(request.getIdentityConfidence());
        marker.setNotifiedChild(request.getNotifiedChild());
        marker.setNavigationStatus(request.getNavigationStatus());
        marker.setObstacleStatus(request.getObstacleStatus());
        marker.setPayloadJson(request.getPayloadJson());
        return marker;
    }

    private InspectionMarker toResponse(RobotMapMarker marker) {
        InspectionMarker response = new InspectionMarker();
        response.setId(marker.getId());
        response.setMapId(marker.getMapId());
        response.setElderId(marker.getElderProfileId());
        response.setType(marker.getMarkerType());
        response.setTitle(marker.getTitle());
        response.setMessage(marker.getDescription());
        response.setDescription(marker.getDescription());
        response.setX(marker.getLocationX());
        response.setY(marker.getLocationY());
        response.setLevel(marker.getLevel());
        response.setStatus(marker.getStatus());
        response.setSource(marker.getSource());
        response.setLocationName(marker.getLocationName());
        response.setElderName(marker.getElderName());
        response.setIdentitySource(marker.getIdentitySource());
        response.setIdentityConfidence(marker.getIdentityConfidence());
        response.setNotifiedChild(marker.getNotifiedChild());
        response.setImageUrl(marker.getImageUrl());
        response.setTime(formatEventTime(marker.getEventTime()));
        response.setNavigationStatus(marker.getNavigationStatus());
        response.setObstacleStatus(marker.getObstacleStatus());
        response.setPayloadJson(marker.getPayloadJson());
        response.setHandler(marker.getHandledByName());
        response.setRemark(marker.getHandleRemark());
        response.setHandleTime(formatHandleTime(marker.getHandledAt()));
        return response;
    }

    private static LocalDateTime parseEventTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : INPUT_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported frontend format.
            }
        }
        return LocalDateTime.parse(value);
    }

    private static String formatEventTime(LocalDateTime value) {
        return value == null ? null : value.format(EVENT_TIME_FORMATTER);
    }

    private static String formatHandleTime(LocalDateTime value) {
        return value == null ? null : value.format(HANDLE_TIME_FORMATTER);
    }

    private static String firstPresent(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
