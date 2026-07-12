package com.smartelderly.api.navigation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotNavigationTaskRepository extends JpaRepository<RobotNavigationTask, Long> {

    Optional<RobotNavigationTask> findFirstByRobotIdAndStatusInOrderByUpdatedAtDescIdDesc(
            Long robotId,
            Collection<String> statuses);

    default Optional<RobotNavigationTask> findLatestActiveTask(Long robotId) {
        return findFirstByRobotIdAndStatusInOrderByUpdatedAtDescIdDesc(
                robotId,
                List.of("running", "pending"));
    }
}
