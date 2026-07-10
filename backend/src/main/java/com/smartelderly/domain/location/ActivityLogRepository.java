package com.smartelderly.domain.location;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Optional<ActivityLog> findFirstByElderProfileIdAndActivityTypeInOrderByStartTimeDesc(
            Long elderProfileId, Collection<String> activityTypes);

    List<ActivityLog> findByElderProfileIdAndActivityTypeInOrderByStartTimeDesc(
            Long elderProfileId, Collection<String> activityTypes, Pageable pageable);
}
