package com.smartelderly.api.medical.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicineReminderViewDto {
    private Long id;
    private Long elderProfileId;
    private String title;
    private String medicineName;
    private String dosage;
    private String frequencyRule;
    private String repeatRule;
    private Boolean enabled;
    private String remindTime;
    private String sourceType;
    private String status;
    private String createdBy;
    private Long relatedEventId;
    private String createdAt;
    private String updatedAt;
}
