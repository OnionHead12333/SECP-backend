package com.smartelderly.api.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建锻炼提醒请求体
 */
@Data
public class ExerciseReminderCreateRequest {
    @NotNull
    private Long elderProfileId;

    @NotBlank
    private String title;

    @NotBlank
    private String exerciseType;

    @NotBlank
    private String startTime;  // HH:mm:ss

    @NotBlank
    private String endTime;    // HH:mm:ss

    @NotNull
    private String remindTime; // ISO8601，如 2026-05-03T04:58:19.510Z

    @NotBlank
    private String repeatRule; // none / daily / weekly

    // 以下字段固定值，由服务端补齐
    // enabled: true
    // sourceType: child_remote
    // status: pending
    // createdBy: child
}
