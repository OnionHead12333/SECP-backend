package com.smartelderly.api.fall;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.dto.ApiResponse;
import com.smartelderly.api.inspection.dto.ChildAlertDto;
import com.smartelderly.api.inspection.dto.FallEventRequest;
import com.smartelderly.api.inspection.dto.FallEventResponse;
import com.smartelderly.service.InspectionMapMockService;

@RestController
@RequestMapping("/v1")
public class FallEventMockController {

    private final InspectionMapMockService inspectionMapMockService;

    public FallEventMockController(InspectionMapMockService inspectionMapMockService) {
        this.inspectionMapMockService = inspectionMapMockService;
    }

    @PostMapping("/fall/events")
    public ApiResponse<FallEventResponse> createFallEvent(@RequestBody FallEventRequest request) {
        FallEventResponse response = inspectionMapMockService.createFallEvent(request);
        if (response == null) {
            return ApiResponse.fail("not fall event");
        }
        return ApiResponse.ok("fall event created", response);
    }

    @GetMapping("/child/alerts")
    public ApiResponse<List<ChildAlertDto>> childAlerts() {
        return ApiResponse.ok(inspectionMapMockService.getChildAlerts());
    }
}
