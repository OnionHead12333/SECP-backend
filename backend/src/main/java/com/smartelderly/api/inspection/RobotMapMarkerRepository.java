package com.smartelderly.api.inspection;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotMapMarkerRepository extends JpaRepository<RobotMapMarker, Long> {

    List<RobotMapMarker> findAllByOrderByIdAsc();
}
