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
public class SuggestedCalendarEventDto {
    private String eventType;
    private String title;
    private LocalDateTime startAt;
    private String notes;
}
