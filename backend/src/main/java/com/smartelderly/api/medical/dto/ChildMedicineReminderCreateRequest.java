package com.smartelderly.api.medical.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ChildMedicineReminderCreateRequest {

    @NotNull
    private Long elderProfileId;

    @NotBlank
    private String title;

    @NotBlank
    private String medicineName;

    private String dosage;

    private String frequencyRule;

    private String repeatRule;

    /** 提醒发生的时刻（UTC，ISO8601 如带 Z）；须与客户端 toUtc().toIso8601String() 一致 */
    @NotNull
    private Instant remindTime;

    @NotNull
    private Boolean enabled;

    private Long relatedEventId;
}
