package com.smartelderly.api.dto.medical;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UpdateMedicalCalendarEventRequest {
    private String eventType;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String notes;
}
