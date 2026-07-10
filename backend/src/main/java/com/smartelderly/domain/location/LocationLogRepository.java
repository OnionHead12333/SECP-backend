package com.smartelderly.domain.location;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationLogRepository extends JpaRepository<LocationLog, Long> {

    // 1. 查询所有记录（按记录时间降序排序）
    List<LocationLog> findByElderProfileIdOrderByRecordedAtDesc(Long elderProfileId);

    // 2. 按时间范围查询（用于查看轨迹回放）
    List<LocationLog> findByElderProfileIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long elderProfileId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    // 3. 性能优化版本：获取最新的位置（仅返回一条）
    Optional<LocationLog> findFirstByElderProfileIdOrderByRecordedAtDesc(Long elderProfileId);
}
