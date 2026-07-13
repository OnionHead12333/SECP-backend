package com.smartelderly.api.inspection;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.security.SecurityUtils;

@RestController
@RequestMapping("/inspection")
public class InspectionMarkerController {

    private final InspectionMarkerService inspectionMarkerService;

    public InspectionMarkerController(InspectionMarkerService inspectionMarkerService) {
        this.inspectionMarkerService = inspectionMarkerService;
    }

    @GetMapping("/map")
    public InspectionApiResponse<InspectionMapInfo> getMap() {
        return InspectionApiResponse.ok(inspectionMarkerService.getMapInfo());
    }

    @GetMapping("/markers")
    public InspectionApiResponse<List<InspectionMarker>> listMarkers() {
        return InspectionApiResponse.ok(inspectionMarkerService.listMarkers());
    }

    @GetMapping("/markers/{id}")
    public InspectionApiResponse<InspectionMarker> getMarker(@PathVariable long id) {
        return inspectionMarkerService.getMarker(id)
                .map(InspectionApiResponse::ok)
                .orElseGet(() -> InspectionApiResponse.fail("marker not found"));
    }

    @PostMapping("/markers")
    public InspectionApiResponse<InspectionMarker> createMarker(@RequestBody InspectionMarker request) {
        var principal = SecurityUtils.requireAuth();
        return InspectionApiResponse.ok(inspectionMarkerService.createMarker(request, principal.userId()));
    }

    @PutMapping("/markers/{id}/handle")
    public InspectionApiResponse<InspectionMarker> handleMarker(
            @PathVariable long id,
            @RequestBody InspectionHandleRequest request) {
        var principal = SecurityUtils.requireAuth();
        return inspectionMarkerService.handleMarker(id, request, principal.userId())
                .map(InspectionApiResponse::ok)
                .orElseGet(() -> InspectionApiResponse.fail("marker not found"));
    }
}
