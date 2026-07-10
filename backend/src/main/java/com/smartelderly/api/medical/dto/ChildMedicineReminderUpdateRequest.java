package com.smartelderly.api.medical.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ChildMedicineReminderUpdateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String medicineName;

    private String dosage;

    private String frequencyRule;

    private String repeatRule;

    @NotNull
    private Instant remindTime;

    @NotNull
    private Boolean enabled;

    private Long relatedEventId;
}
