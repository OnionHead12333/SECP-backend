package com.smartelderly.api.child;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.InspectionApiResponse;

@RestController
@RequestMapping("/child/fall-alerts")
public class ChildFallAlertController {

    private final ChildFallAlertService childFallAlertService;

    public ChildFallAlertController(ChildFallAlertService childFallAlertService) {
        this.childFallAlertService = childFallAlertService;
    }

    @GetMapping
    public InspectionApiResponse<List<ChildFallAlert>> listFallAlerts(@RequestParam Long childUserId) {
        return InspectionApiResponse.ok(childFallAlertService.listFallAlerts(childUserId));
    }

    @GetMapping("/{id}")
    public InspectionApiResponse<ChildFallAlert> getFallAlert(
            @PathVariable long id,
            @RequestParam Long childUserId) {
        return childFallAlertService.getFallAlert(id, childUserId)
                .map(InspectionApiResponse::ok)
                .orElseGet(() -> InspectionApiResponse.fail("fall alert not found"));
    }
}
