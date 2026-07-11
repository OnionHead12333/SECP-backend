package com.smartelderly.api.inspection;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.dto.ApiResponse;
import com.smartelderly.api.inspection.dto.InspectionMarkerDto;
import com.smartelderly.api.inspection.dto.InspectionPlaceDto;
import com.smartelderly.api.inspection.dto.InspectionRouteDto;
import com.smartelderly.api.inspection.dto.MapInfoDto;
import com.smartelderly.api.inspection.dto.MarkerHandleRequest;
import com.smartelderly.service.InspectionMapMockService;

@RestController
@RequestMapping("/v1/inspection")
public class InspectionMapMockController {

    private final InspectionMapMockService inspectionMapMockService;

    public InspectionMapMockController(InspectionMapMockService inspectionMapMockService) {
        this.inspectionMapMockService = inspectionMapMockService;
    }

    @GetMapping("/map")
    public ApiResponse<MapInfoDto> mapInfo() {
        return ApiResponse.ok(inspectionMapMockService.getMapInfo());
    }

    @GetMapping("/places")
    public ApiResponse<List<InspectionPlaceDto>> places() {
        return ApiResponse.ok(inspectionMapMockService.getPlaces());
    }

    @GetMapping("/routes")
    public ApiResponse<List<InspectionRouteDto>> routes() {
        return ApiResponse.ok(inspectionMapMockService.getRoutes());
    }

    @GetMapping("/markers")
    public ApiResponse<List<InspectionMarkerDto>> markers() {
        return ApiResponse.ok(inspectionMapMockService.getMarkers());
    }

    @PostMapping("/markers")
    public ApiResponse<InspectionMarkerDto> createMarker(@RequestBody InspectionMarkerDto request) {
        return ApiResponse.ok("marker created", inspectionMapMockService.createMarker(request));
    }

    @GetMapping("/markers/{id}")
    public ApiResponse<InspectionMarkerDto> markerDetail(@PathVariable("id") long id) {
        try {
            return ApiResponse.ok(inspectionMapMockService.getMarker(id));
        } catch (NoSuchElementException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @PutMapping("/markers/{id}/handle")
    public ApiResponse<InspectionMarkerDto> handleMarker(
            @PathVariable("id") long id,
            @RequestBody(required = false) MarkerHandleRequest request) {
        try {
            return ApiResponse.ok("marker handled", inspectionMapMockService.handleMarker(id));
        } catch (NoSuchElementException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }
}
