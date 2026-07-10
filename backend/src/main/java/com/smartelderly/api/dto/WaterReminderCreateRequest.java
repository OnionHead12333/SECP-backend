package com.smartelderly.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 子女端创建喝水提醒请求 DTO
 */
@Data
public class WaterReminderCreateRequest {

    @NotNull(message = "老人档案ID不能为空")
    private Long elderProfileId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotNull(message = "每日目标饮水量不能为空")
    @Min(value = 100, message = "每日目标至少100ml")
    @Max(value = 5000, message = "每日目标最多5000ml")
    private Integer dailyTargetMl;

    @NotNull(message = "提醒间隔不能为空")
    @Min(value = 1, message = "间隔至少1分钟")
    @Max(value = 480, message = "间隔最多480分钟")
    private Integer intervalMinutes;

    /** 开始时间 HH:mm:ss */
    private String startTime;

    /** 结束时间 HH:mm:ss */
    private String endTime;

    /** 提醒时间 UTC ISO8601 */
    @NotBlank(message = "提醒时间不能为空")
    private String remindTime;

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @NotBlank(message = "状态不能为空")
    private String status;

    @NotBlank(message = "创建者不能为空")
    private String createdBy;
}
