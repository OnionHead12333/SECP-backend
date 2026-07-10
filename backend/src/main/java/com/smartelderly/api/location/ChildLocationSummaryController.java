package com.smartelderly.api.location;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.LocationSummaryResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.location.LocationSummaryService;

@RestController
@RequestMapping("/v1/child/elders")
public class ChildLocationSummaryController {

    private final LocationSummaryService locationSummaryService;

    public ChildLocationSummaryController(LocationSummaryService locationSummaryService) {
        this.locationSummaryService = locationSummaryService;
    }

    /**
     * 获取定位摘要
     * @param elderId 老人ID
     * @return 位置摘要信息
     */
    @GetMapping("/{elderId}/location-summary")
    public ApiResponse<LocationSummaryResponse> getLocationSummary(@PathVariable("elderId") Long elderId) {
        var user = SecurityUtils.requireRole(UserRole.child);
        return locationSummaryService.getLocationSummary(elderId);
    }
}
