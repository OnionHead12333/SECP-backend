package com.smartelderly.domain.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeofenceRepository extends JpaRepository<Geofence, Long> {
    /**
     * 根据老人ID和围栏名称查询围栏
     */
    Optional<Geofence> findByElderProfileIdAndName(Long elderProfileId, String name);

    /**
     * 根据老人ID查询所有围栏
     */
    List<Geofence> findByElderProfileId(Long elderProfileId);

    /**
     * 根据老人ID和启用状态查询所有围栏
     */
    List<Geofence> findByElderProfileIdAndIsEnabled(Long elderProfileId, Boolean isEnabled);
}
