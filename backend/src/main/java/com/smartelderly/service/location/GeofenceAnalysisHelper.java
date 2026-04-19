package com.smartelderly.service.location;

import java.util.List;

import org.springframework.stereotype.Component;

import com.smartelderly.domain.location.Geofence;
import com.smartelderly.domain.location.LocationLog;

/**
 * 地理围栏分析工具类
 * 提供位置与围栏的距离计算和在家判定逻辑
 */
@Component
public class GeofenceAnalysisHelper {

    // 地球半径（单位：米）
    private static final int EARTH_RADIUS_METERS = 6371000;

    /**
     * 判断位置是否在任何启用的围栏范围内
     * 只要位置在任何一个围栏范围内，就判断为在家
     *
     * @param location 位置记录
     * @param enabledGeofences 启用的围栏列表
     * @return true 表示在家（在某个围栏范围内），false 表示外出
     */
    public boolean isLocationWithinGeofences(LocationLog location, List<Geofence> enabledGeofences) {
        if (location == null || enabledGeofences == null || enabledGeofences.isEmpty()) {
            return false;
        }

        for (Geofence geofence : enabledGeofences) {
            double distance = calculateDistance(
                    location.getLatitude().doubleValue(),
                    location.getLongitude().doubleValue(),
                    geofence.getCenterLatitude().doubleValue(),
                    geofence.getCenterLongitude().doubleValue()
            );

            // 只要在任何一个围栏范围内就返回 true
            if (distance <= geofence.getRadius()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 使用Haversine公式计算两点间距离（单位：米）
     *
     * @param lat1 点1的纬度
     * @param lon1 点1的经度
     * @param lat2 点2的纬度
     * @param lon2 点2的经度
     * @return 两点间距离，单位：米
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
