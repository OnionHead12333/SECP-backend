package com.smartelderly.api.inspection;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RobotMapMarkerRepository extends JpaRepository<RobotMapMarker, Long> {

    List<RobotMapMarker> findAllByOrderByIdAsc();

    @Query(value = """
            select m.*
            from robot_map_marker m
            join family_bindings fb on fb.elder_profile_id = m.elder_profile_id
            where m.marker_type = 'fall'
                and fb.child_user_id = :childUserId
                and fb.status = 'active'
            order by
                case when m.status = 'unhandled' then 0 else 1 end,
                coalesce(m.event_time, m.created_at) desc,
                m.id desc
            """, nativeQuery = true)
    List<RobotMapMarker> findFallAlertsForChild(@Param("childUserId") Long childUserId);

    @Query(value = """
            select m.*
            from robot_map_marker m
            join family_bindings fb on fb.elder_profile_id = m.elder_profile_id
            where m.id = :id
                and m.marker_type = 'fall'
                and fb.child_user_id = :childUserId
                and fb.status = 'active'
            """, nativeQuery = true)
    Optional<RobotMapMarker> findFallAlertByIdAndChildUserId(
            @Param("id") Long id,
            @Param("childUserId") Long childUserId);
}
