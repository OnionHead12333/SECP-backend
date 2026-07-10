package com.smartelderly.api.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 老人端锻炼提醒今日进度（与 Flutter ElderExerciseProgress 字段对齐）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseReminderProgressResponse {

    private int plannedCount;
    private int completedCount;
    private int missedCount;
    private int pendingCount;
    private String lastCompletionStatus;
    private String lastCompletionSource;
    private Long activeReminderId;
    /** 下一次锻炼提醒时刻（UTC ISO-8601），与喝水进度字段对齐，供老人端前台调度弹窗 */
    private String nextReminderAt;
    private String lastCompletedAt;
}
