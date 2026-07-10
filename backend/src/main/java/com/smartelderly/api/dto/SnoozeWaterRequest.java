package com.smartelderly.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 稍后提醒请求 DTO
 */
@Data
public class SnoozeWaterRequest {

    @NotNull(message = "老人档案ID不能为空")
    private Long elderId;

    @Min(value = 1, message = "顺延分钟数至少为1")
    private int snoozeMinutes;

    @NotNull(message = "发起稍后提醒时间不能为空")
    private String requestedAt;
}
