package com.smartelderly.api.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改锻炼提醒请求体
 */
@Data
public class ExerciseReminderUpdateRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String exerciseType;

    @NotBlank
    private String repeatRule;

    @NotBlank
    private String startTime;  // HH:mm:ss

    @NotBlank
    private String endTime;    // HH:mm:ss
}
