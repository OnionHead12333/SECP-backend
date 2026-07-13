package com.smartelderly.api.child;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.InspectionApiResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;

@RestController
@RequestMapping("/child/fall-alerts")
public class ChildFallAlertController {

    private final ChildFallAlertService childFallAlertService;

    public ChildFallAlertController(ChildFallAlertService childFallAlertService) {
        this.childFallAlertService = childFallAlertService;
    }

    @GetMapping
    public InspectionApiResponse<List<ChildFallAlert>> listFallAlerts(
            @RequestParam(required = false) Long childUserId) {
        var principal = SecurityUtils.requireRole(UserRole.child);
        SecurityUtils.requireMatchingUserId(principal, childUserId);
        return InspectionApiResponse.ok(childFallAlertService.listFallAlerts(principal.userId()));
    }

    @GetMapping("/{id}")
    public InspectionApiResponse<ChildFallAlert> getFallAlert(
            @PathVariable long id,
            @RequestParam(required = false) Long childUserId) {
        var principal = SecurityUtils.requireRole(UserRole.child);
        SecurityUtils.requireMatchingUserId(principal, childUserId);
        return childFallAlertService.getFallAlert(id, principal.userId())
                .map(InspectionApiResponse::ok)
                .orElseGet(() -> InspectionApiResponse.fail("fall alert not found"));
    }
}
