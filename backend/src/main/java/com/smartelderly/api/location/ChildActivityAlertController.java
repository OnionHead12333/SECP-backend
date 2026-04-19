package com.smartelderly.api.location;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.ActivityAlertResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.location.ActivityAlertService;

@RestController
@RequestMapping("/v1/child/elders")
public class ChildActivityAlertController {

    private final ActivityAlertService activityAlertService;

    public ChildActivityAlertController(ActivityAlertService activityAlertService) {
        this.activityAlertService = activityAlertService;
    }

    /**
     * 获取未活动提醒列表
     * @param elderId 老人ID
     * @return 未活动提醒列表
     */
    @GetMapping("/{elderId}/activity-alerts")
    public ApiResponse<List<ActivityAlertResponse>> getActivityAlerts(@PathVariable("elderId") Long elderId) {
        var user = SecurityUtils.requireRole(UserRole.child);
        return activityAlertService.getActivityAlerts(elderId);
    }
}
