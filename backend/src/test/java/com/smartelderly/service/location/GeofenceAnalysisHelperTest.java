package com.smartelderly.service.location;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.domain.location.Geofence;
import com.smartelderly.domain.location.LocationLog;

/**
 * GeofenceAnalysisHelper 的单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("地理围栏分析工具测试")
public class GeofenceAnalysisHelperTest {

    @InjectMocks
    private GeofenceAnalysisHelper geofenceAnalysisHelper;

    private LocationLog location;
    private List<Geofence> geofences;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        location = new LocationLog();
        location.setLatitude(new BigDecimal("31.230500"));
        location.setLongitude(new BigDecimal("121.473900"));

        geofences = new ArrayList<>();
    }

    @Test
    @DisplayName("测试：位置在围栏范围内，应返回 true")
    public void testIsLocationWithinGeofences_LocationInside() {
        // 给定：一个围栏（中心点：31.230400, 121.473700，半径：300米）
        Geofence geofence = new Geofence();
        geofence.setCenterLatitude(new BigDecimal("31.230400"));
        geofence.setCenterLongitude(new BigDecimal("121.473700"));
        geofence.setRadius(300);
        geofences.add(geofence);

        // 当：判断位置是否在围栏内
        boolean result = geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences);

        // 那么：应该返回 true
        assertTrue(result, "位置应该在围栏范围内");
    }

    @Test
    @DisplayName("测试：位置不在围栏范围内，应返回 false")
    public void testIsLocationWithinGeofences_LocationOutside() {
        // 给定：一个围栏（中心点：31.200000, 121.400000，半径：100米）
        Geofence geofence = new Geofence();
        geofence.setCenterLatitude(new BigDecimal("31.200000"));
        geofence.setCenterLongitude(new BigDecimal("121.400000"));
        geofence.setRadius(100);
        geofences.add(geofence);

        // 当：判断位置是否在围栏内
        boolean result = geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences);

        // 那么：应该返回 false
        assertFalse(result, "位置应该不在围栏范围内");
    }

    @Test
    @DisplayName("测试：多个围栏，位置在其中一个范围内，应返回 true")
    public void testIsLocationWithinGeofences_MultipleGeofences() {
        // 给定：两个围栏，位置在第二个围栏内
        Geofence geofence1 = new Geofence();
        geofence1.setCenterLatitude(new BigDecimal("31.100000"));
        geofence1.setCenterLongitude(new BigDecimal("121.300000"));
        geofence1.setRadius(100);
        geofences.add(geofence1);

        Geofence geofence2 = new Geofence();
        geofence2.setCenterLatitude(new BigDecimal("31.230400"));
        geofence2.setCenterLongitude(new BigDecimal("121.473700"));
        geofence2.setRadius(300);
        geofences.add(geofence2);

        // 当：判断位置是否在任何一个围栏内
        boolean result = geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences);

        // 那么：应该返回 true
        assertTrue(result, "位置应该在第二个围栏范围内");
    }

    @Test
    @DisplayName("测试：围栏列表为空，应返回 false")
    public void testIsLocationWithinGeofences_EmptyGeofenceList() {
        // 给定：空的围栏列表
        geofences = new ArrayList<>();

        // 当：判断位置是否在围栏内
        boolean result = geofenceAnalysisHelper.isLocationWithinGeofences(location, geofences);

        // 那么：应该返回 false
        assertFalse(result, "围栏列表为空时应返回 false");
    }

    @Test
    @DisplayName("测试：位置为 null，应返回 false")
    public void testIsLocationWithinGeofences_LocationNull() {
        // 给定：位置为 null，围栏列表有数据
        Geofence geofence = new Geofence();
        geofence.setCenterLatitude(new BigDecimal("31.230400"));
        geofence.setCenterLongitude(new BigDecimal("121.473700"));
        geofence.setRadius(300);
        geofences.add(geofence);

        // 当：判断 null 位置是否在围栏内
        boolean result = geofenceAnalysisHelper.isLocationWithinGeofences(null, geofences);

        // 那么：应该返回 false
        assertFalse(result, "位置为 null 时应返回 false");
    }

    @Test
    @DisplayName("测试：距离计算 - 同一位置")
    public void testCalculateDistance_SameLocation() {
        // 给定：同一位置的经纬度
        double lat = 31.230400;
        double lon = 121.473700;

        // 当：计算距离
        double distance = geofenceAnalysisHelper.calculateDistance(lat, lon, lat, lon);

        // 那么：距离应该接近 0
        assertEquals(0.0, distance, 0.1, "同一位置的距离应该接近 0");
    }

    @Test
    @DisplayName("测试：距离计算 - 不同位置")
    public void testCalculateDistance_DifferentLocations() {
        // 给定：两个不同的位置
        double lat1 = 31.230400;
        double lon1 = 121.473700;
        double lat2 = 31.230500;
        double lon2 = 121.473900;

        // 当：计算距离
        double distance = geofenceAnalysisHelper.calculateDistance(lat1, lon1, lat2, lon2);

        // 那么：距离应该大于 0，且是合理的范围（预期应在几十米以内）
        assertTrue(distance > 0, "不同位置的距离应该大于 0");
        assertTrue(distance < 200, "相近位置的距离应该在 200 米以内");
    }
}
