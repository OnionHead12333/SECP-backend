package com.smartelderly.api.navigation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.InspectionApiResponse;

@RestController
@RequestMapping("/navigation")
public class NavigationStatusController {

    @GetMapping("/status")
    public InspectionApiResponse<NavigationStatus> status() {
        return InspectionApiResponse.ok(new NavigationStatus(
                "running",
                100,
                120,
                420,
                210,
                "elder_room",
                "\u6b63\u5728\u524d\u5f80\u8001\u4eba\u623f\u95f4"));
    }

    public record NavigationStatus(
            String navigationStatus,
            int currentX,
            int currentY,
            int targetX,
            int targetY,
            String targetName,
            String message) {
    }
}
