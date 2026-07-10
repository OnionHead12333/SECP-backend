package com.smartelderly.api.dto.medical;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class CreateMedicalCalendarEventRequest {
    /** 子女必填；老人登录时可省略，由服务端解析本人档案。 */
    private Long elderProfileId;

    @NotBlank private String eventType;

    @NotBlank private String title;

    @NotNull private LocalDateTime startAt;

    private LocalDateTime endAt;
    private String notes;
    private Long sourceDocumentId;
}
