package com.smartelderly.api.entertainment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotEntertainmentTaskRepository extends JpaRepository<RobotEntertainmentTask, Long> {

    List<RobotEntertainmentTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<RobotEntertainmentTask> findByStatusOrderByCreatedAtAsc(String status);

    Optional<RobotEntertainmentTask> findFirstByOrderByCreatedAtDesc();
}
