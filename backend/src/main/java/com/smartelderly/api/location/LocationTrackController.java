package com.smartelderly.api.location;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.LocationTrackDTO;
import com.smartelderly.domain.ElderProfile;

import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.UserRole;
import com.smartelderly.domain.location.LocationLog;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.location.LocationTrackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/elder/location-tracks")
@Validated//启用方法参数验证
public class LocationTrackController {

    private final LocationTrackService locationTrackService;
    private final ElderProfileRepository elderProfileRepository;

    // 构造函数，使用构造函数注入LocationTrackService和ElderProfileRepository（准备好Service和Repository，前端调用时调用Service的方法获取数据）
    public LocationTrackController(LocationTrackService locationTrackService,
                                   ElderProfileRepository elderProfileRepository) {
        this.locationTrackService = locationTrackService;
        this.elderProfileRepository = elderProfileRepository;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> uploadLocation(@Valid @RequestBody LocationTrackDTO dto) {
        // 第1步：通过SecurityUtils获取当前登录的老人用户
        var user = SecurityUtils.requireRole(UserRole.elder);
        
        // 第2步：根据登录用户ID查询老人档案，获取elderProfileId
        ElderProfile elder = elderProfileRepository
                .findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));
        
        // 第3步：调用Service，传入elderProfileId和定位数据
        LocationLog log = locationTrackService.saveLocation(elder.getId(), dto);
        // 第4步：返回成功响应，包含新记录的ID
        Map<String, Object> data = Map.of("locationId", log.getId());
        return ApiResponse.ok("uploaded", data);
    }
}
