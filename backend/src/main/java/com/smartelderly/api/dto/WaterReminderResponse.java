package com.smartelderly.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 喝水提醒响应 DTO（子女端列表项 + 老人端列表项）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WaterReminderResponse {

    private Long id;

    private Long elderProfileId;

    private String title;

    private Integer dailyTargetMl;

    private Integer intervalMinutes;

    private String startTime;

    private String endTime;

    private Integer todayIntakeMl;

    private String lastIntakeTime;

    private String sourceType;

    private String remindTime;

    private String repeatRule;

    private Boolean enabled;

    private String status;

    private String createdBy;

    private String createdAt;

    private String updatedAt;
}
