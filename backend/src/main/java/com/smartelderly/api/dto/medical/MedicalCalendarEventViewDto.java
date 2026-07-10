package com.smartelderly.api.dto.medical;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalCalendarEventViewDto {
    private Long id;
    private Long elderProfileId;
    private String eventType;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String notes;
    private Long sourceDocumentId;
    private LocalDateTime createdAt;
}
