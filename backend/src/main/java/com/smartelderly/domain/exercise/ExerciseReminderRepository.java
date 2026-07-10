package com.smartelderly.domain.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 锻炼提醒 Repository
 */
@Repository
public interface ExerciseReminderRepository extends JpaRepository<ExerciseReminder, Long> {
    /**
     * 按老人档案 ID 查询，按创建时间倒序
     */
    List<ExerciseReminder> findByElderProfileIdOrderByCreatedAtDesc(Long elderProfileId);

    List<ExerciseReminder> findByElderProfileIdAndEnabledTrueOrderByIdAsc(Long elderProfileId);
}
