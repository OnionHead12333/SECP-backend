package com.smartelderly.service.location;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.LocationSummaryResponse;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.location.GeofenceRepository;
import com.smartelderly.domain.location.LocationLogRepository;

@Service
public class LocationSummaryService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final String GAODE_FALLBACK = "gaode_fallback";
    private static final String GAODE_CURRENT = "gaode_current";

    private final LocationLogRepository locationLogRepository;
    private final GeofenceRepository geofenceRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final GeofenceAnalysisHelper geofenceAnalysisHelper;

    public LocationSummaryService(LocationLogRepository locationLogRepository,
            GeofenceRepository geofenceRepository,
            ElderProfileRepository elderProfileRepository,
            GeofenceAnalysisHelper geofenceAnalysisHelper) {
        this.locationLogRepository = locationLogRepository;
        this.geofenceRepository = geofenceRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.geofenceAnalysisHelper = geofenceAnalysisHelper;
    }

    /**
     * 获取老人的定位摘要
     * @param elderId 老人ID
     * @return ApiResponse 包含最新位置和状态信息
     */
    public ApiResponse<LocationSummaryResponse> getLocationSummary(Long elderId) {
        // 验证老人是否存在
        var elder = elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 获取最新的位置记录
        var latestLocation = locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(elderId)
                .orElseThrow(() -> new ApiException(4050, "location not found"));

        // 判断是否在家（检查所有启用的围栏，只要在其中一个范围内即为在家）
        var enabledGeofences = geofenceRepository.findByElderProfileIdAndIsEnabled(elderId, true);
        boolean isHome = geofenceAnalysisHelper.isLocationWithinGeofences(latestLocation, enabledGeofences);

        // 设置定位来源
        String presenceSource = isHome ? GAODE_CURRENT : GAODE_FALLBACK;

        // 构造响应
        LocationSummaryResponse response = new LocationSummaryResponse(
                latestLocation.getLatitude(),
                latestLocation.getLongitude(),
                "", // 地址暂不填充，可后续扩展
                isHome,
                presenceSource,
                latestLocation.getRecordedAt().format(ISO_FORMATTER)
        );

        return ApiResponse.ok(response);
    }
}
