package com.smartelderly.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WaterReminderRepository extends JpaRepository<WaterReminder, Long> {

    /** 按老人档案ID查询所有喝水提醒，按创建时间降序 */
    List<WaterReminder> findByElderProfileIdOrderByCreatedAtDesc(Long elderProfileId);

    /** 按老人档案ID和启用状态查询 */
    List<WaterReminder> findByElderProfileIdAndEnabledTrue(Long elderProfileId);

    /** 查询某个提醒及其所属老人 */
    Optional<WaterReminder> findByIdAndElderProfileId(Long id, Long elderProfileId);

    /** 查询某老人今天启用的喝水提醒，按 remindTime 升序 */
    @Query("SELECT w FROM WaterReminder w WHERE w.elderProfileId = :elderId " +
           "AND w.enabled = true ORDER BY w.remindTime ASC")
    List<WaterReminder> findEnabledByElderProfileId(@Param("elderId") Long elderId);
}
