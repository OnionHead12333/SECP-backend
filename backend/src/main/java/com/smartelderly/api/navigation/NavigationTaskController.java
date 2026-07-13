package com.smartelderly.api.navigation;

import com.smartelderly.api.inspection.InspectionApiResponse;
import com.smartelderly.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/navigation")
public class NavigationTaskController {

    private final NavigationTaskService navigationTaskService;

    public NavigationTaskController(NavigationTaskService navigationTaskService) {
        this.navigationTaskService = navigationTaskService;
    }

    @PostMapping("/tasks")
    public InspectionApiResponse<NavigationTaskResponse> createTask(@RequestBody NavigationTaskRequest request) {
        var principal = SecurityUtils.requireMatchingUserId(request.creatorId());
        NavigationTaskRequest authenticatedRequest = new NavigationTaskRequest(
                request.robotId(),
                principal.userId(),
                request.mapId(),
                request.targetName(),
                request.targetX(),
                request.targetY());
        return InspectionApiResponse.ok(navigationTaskService.createTask(authenticatedRequest));
    }

    @GetMapping("/status")
    public InspectionApiResponse<NavigationStatusResponse> getStatus() {
        return InspectionApiResponse.ok(navigationTaskService.getStatus());
    }

    @PostMapping("/tasks/{id}/cancel")
    public InspectionApiResponse<NavigationTaskResponse> cancelTask(@PathVariable long id) {
        var principal = SecurityUtils.requireAuth();
        return navigationTaskService.cancelTask(id, principal.userId())
                .map(InspectionApiResponse::ok)
                .orElseGet(() -> InspectionApiResponse.fail("navigation task not found"));
    }
}
