package com.smartelderly.service.location;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.LocationSummaryResponse;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.location.Geofence;
import com.smartelderly.domain.location.GeofenceRepository;
import com.smartelderly.domain.location.LocationLog;
import com.smartelderly.domain.location.LocationLogRepository;

/**
 * LocationSummaryService 的单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("定位摘要服务测试")
public class LocationSummaryServiceTest {

    @Mock
    private LocationLogRepository locationLogRepository;

    @Mock
    private GeofenceRepository geofenceRepository;

    @Mock
    private ElderProfileRepository elderProfileRepository;

    @Mock
    private GeofenceAnalysisHelper geofenceAnalysisHelper;

    @InjectMocks
    private LocationSummaryService locationSummaryService;

    private Long elderId;
    private ElderProfile elder;
    private LocationLog latestLocation;
    private List<Geofence> geofences;

    @BeforeEach
    public void setUp() {
        elderId = 1L;

        // 初始化老人信息
        elder = new ElderProfile();
        elder.setId(elderId);

        // 初始化最新位置
        latestLocation = new LocationLog();
        latestLocation.setId(1L);
        latestLocation.setElderProfileId(elderId);
        latestLocation.setLatitude(new BigDecimal("31.230500"));
        latestLocation.setLongitude(new BigDecimal("121.473900"));
        latestLocation.setRecordedAt(LocalDateTime.now());

        // 初始化围栏列表
        geofences = new ArrayList<>();
    }

    @Test
    @DisplayName("测试：成功获取定位摘要（在家）")
    public void testGetLocationSummary_Success_AtHome() {
        // 给定：老人存在，有最新位置，判定为在家
        when(elderProfileRepository.findById(elderId)).thenReturn(Optional.of(elder));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(elderId))
                .thenReturn(Optional.of(latestLocation));
        when(geofenceRepository.findByElderProfileIdAndIsEnabled(elderId, true))
                .thenReturn(geofences);
        when(geofenceAnalysisHelper.isLocationWithinGeofences(latestLocation, geofences))
                .thenReturn(true);

        // 当：获取定位摘要
        ApiResponse<LocationSummaryResponse> response = locationSummaryService.getLocationSummary(elderId);

        // 那么：应该返回成功响应，isHome 为 true
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertEquals(LocationSummaryResponse.class, response.getData().getClass());
        assertTrue(response.getData().getIsHome());
        assertEquals("gaode_current", response.getData().getPresenceSource());

        // 验证调用
        verify(elderProfileRepository, times(1)).findById(elderId);
        verify(locationLogRepository, times(1)).findFirstByElderProfileIdOrderByRecordedAtDesc(elderId);
        verify(geofenceRepository, times(1)).findByElderProfileIdAndIsEnabled(elderId, true);
    }

    @Test
    @DisplayName("测试：成功获取定位摘要（外出）")
    public void testGetLocationSummary_Success_Away() {
        // 给定：老人存在，有最新位置，判定为外出
        when(elderProfileRepository.findById(elderId)).thenReturn(Optional.of(elder));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(elderId))
                .thenReturn(Optional.of(latestLocation));
        when(geofenceRepository.findByElderProfileIdAndIsEnabled(elderId, true))
                .thenReturn(geofences);
        when(geofenceAnalysisHelper.isLocationWithinGeofences(latestLocation, geofences))
                .thenReturn(false);

        // 当：获取定位摘要
        ApiResponse<LocationSummaryResponse> response = locationSummaryService.getLocationSummary(elderId);

        // 那么：应该返回成功响应，isHome 为 false
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertFalse(response.getData().getIsHome());
        assertEquals("gaode_fallback", response.getData().getPresenceSource());
    }

    @Test
    @DisplayName("测试：老人不存在时抛出异常")
    public void testGetLocationSummary_ElderNotFound() {
        // 给定：老人不存在
        when(elderProfileRepository.findById(elderId)).thenReturn(Optional.empty());

        // 当&那么：应该抛出 ApiException
        ApiException exception = assertThrows(ApiException.class, () -> {
            locationSummaryService.getLocationSummary(elderId);
        });

        assertEquals(4040, exception.getCode());
        assertEquals("elder not found", exception.getMessage());
    }

    @Test
    @DisplayName("测试：位置记录不存在时抛出异常")
    public void testGetLocationSummary_LocationNotFound() {
        // 给定：老人存在，但没有位置记录
        when(elderProfileRepository.findById(elderId)).thenReturn(Optional.of(elder));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(elderId))
                .thenReturn(Optional.empty());

        // 当&那么：应该抛出 ApiException
        ApiException exception = assertThrows(ApiException.class, () -> {
            locationSummaryService.getLocationSummary(elderId);
        });

        assertEquals(4050, exception.getCode());
        assertEquals("location not found", exception.getMessage());
    }

    @Test
    @DisplayName("测试：响应数据完整性")
    public void testGetLocationSummary_ResponseDataCompleteness() {
        // 给定：所有依赖都已 mock
        when(elderProfileRepository.findById(elderId)).thenReturn(Optional.of(elder));
        when(locationLogRepository.findFirstByElderProfileIdOrderByRecordedAtDesc(elderId))
                .thenReturn(Optional.of(latestLocation));
        when(geofenceRepository.findByElderProfileIdAndIsEnabled(elderId, true))
                .thenReturn(geofences);
        when(geofenceAnalysisHelper.isLocationWithinGeofences(latestLocation, geofences))
                .thenReturn(true);

        // 当：获取定位摘要
        ApiResponse<LocationSummaryResponse> response = locationSummaryService.getLocationSummary(elderId);

        // 那么：响应数据应包含所有必要字段
        LocationSummaryResponse data = response.getData();
        assertNotNull(data.getLatitude());
        assertNotNull(data.getLongitude());
        assertNotNull(data.getIsHome());
        assertNotNull(data.getPresenceSource());
        assertNotNull(data.getUpdatedAt());
    }
}
