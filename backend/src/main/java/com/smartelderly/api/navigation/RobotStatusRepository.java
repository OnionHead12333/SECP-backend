package com.smartelderly.api.navigation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotStatusRepository extends JpaRepository<RobotStatus, Long> {

    Optional<RobotStatus> findByRobotId(Long robotId);

    Optional<RobotStatus> findFirstByOrderByUpdatedAtDesc();
}
