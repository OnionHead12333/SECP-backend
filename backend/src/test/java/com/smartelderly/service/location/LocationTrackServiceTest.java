package com.smartelderly.service.location;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.api.location.dto.LocationTrackDTO;
import com.smartelderly.domain.location.LocationLog;
import com.smartelderly.domain.location.LocationLogRepository;
import com.smartelderly.domain.location.LocationSource;
import com.smartelderly.domain.location.LocationType;

@ExtendWith(MockitoExtension.class)
@DisplayName("定位轨迹服务单元测试")
class LocationTrackServiceTest {

    @Mock
    private LocationLogRepository locationLogRepository;

    @Mock
    private ElderLocationGuardService elderLocationGuardService;

    @InjectMocks
    private LocationTrackService locationTrackService;

    @Test
    @DisplayName("保存定位：合法枚举和 UTC 时间应正确映射并回调守护状态")
    void saveLocation_validPayload_mapsFieldsAndMarksUploadSuccess() {
        LocationTrackDTO dto = new LocationTrackDTO();
        dto.setLatitude(new BigDecimal("39.9042"));
        dto.setLongitude(new BigDecimal("116.4074"));
        dto.setLocationType("indoor");
        dto.setSource("gps");
        dto.setRecordedAt("2026-06-06T10:00:00Z");

        when(locationLogRepository.save(any(LocationLog.class))).thenAnswer(invocation -> {
            LocationLog log = invocation.getArgument(0);
            log.setId(100L);
            return log;
        });

        LocationLog saved = locationTrackService.saveLocation(7L, dto);

        assertEquals(100L, saved.getId());
        assertEquals(7L, saved.getElderProfileId());
        assertEquals(new BigDecimal("39.9042"), saved.getLatitude());
        assertEquals(new BigDecimal("116.4074"), saved.getLongitude());
        assertEquals(LocationType.indoor, saved.getLocationType());
        assertEquals(LocationSource.gps, saved.getSource());
        assertEquals(LocalDateTime.of(2026, 6, 6, 18, 0), saved.getRecordedAt());
        verify(elderLocationGuardService).markUploadSuccess(7L, saved.getRecordedAt());
    }

    @Test
    @DisplayName("保存定位：非法枚举应回退到默认 outdoor/gaode")
    void saveLocation_invalidEnums_usesDefaults() {
        LocationTrackDTO dto = new LocationTrackDTO();
        dto.setLatitude(new BigDecimal("31.2305"));
        dto.setLongitude(new BigDecimal("121.4739"));
        dto.setLocationType("car");
        dto.setSource("unknown");
        dto.setRecordedAt("2026-06-06T18:30:00");

        when(locationLogRepository.save(any(LocationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocationLog saved = locationTrackService.saveLocation(9L, dto);

        assertEquals(LocationType.outdoor, saved.getLocationType());
        assertEquals(LocationSource.gaode, saved.getSource());
        assertEquals(LocalDateTime.of(2026, 6, 6, 18, 30), saved.getRecordedAt());
    }

    @Test
    @DisplayName("保存定位：空枚举和空时间应使用默认值")
    void saveLocation_nullOptionalFields_usesDefaultsAndCurrentTime() {
        LocationTrackDTO dto = new LocationTrackDTO();
        dto.setLatitude(new BigDecimal("31.2305"));
        dto.setLongitude(new BigDecimal("121.4739"));

        when(locationLogRepository.save(any(LocationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocationLog saved = locationTrackService.saveLocation(11L, dto);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertEquals(LocationType.outdoor, saved.getLocationType());
        assertEquals(LocationSource.gaode, saved.getSource());
        assertNotNull(saved.getRecordedAt());
        assertFalse(saved.getRecordedAt().isBefore(before));
        assertFalse(saved.getRecordedAt().isAfter(after));

        ArgumentCaptor<LocationLog> captor = ArgumentCaptor.forClass(LocationLog.class);
        verify(locationLogRepository).save(captor.capture());
        assertEquals(11L, captor.getValue().getElderProfileId());
    }
}
