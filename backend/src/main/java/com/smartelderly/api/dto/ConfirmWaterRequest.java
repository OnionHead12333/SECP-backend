package com.smartelderly.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 确认喝水请求 DTO
 */
@Data
public class ConfirmWaterRequest {

    @NotNull(message = "老人档案ID不能为空")
    private Long elderId;

    @NotNull(message = "确认时间不能为空")
    private String confirmedAt;
}
