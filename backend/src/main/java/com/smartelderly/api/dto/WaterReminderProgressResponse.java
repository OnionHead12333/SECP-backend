package com.smartelderly.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 喝水提醒进度响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WaterReminderProgressResponse {

    /** 今日计划提醒次数 */
    private int plannedCount;

    /** 今日已确认喝水次数 */
    private int confirmedCount;

    /** 今日错过次数 */
    private int missedCount;

    /** 今日待完成次数 */
    private int pendingCount;

    /** 完成百分比，0~100 */
    private double completionPercent;

    /** 当前活跃提醒 ID，无则返回 0 */
    private Long activeReminderId;

    /** 最近一次喝水确认时间 */
    private String lastConfirmedAt;

    /** 下一次提醒时间 */
    private String nextReminderAt;
}
