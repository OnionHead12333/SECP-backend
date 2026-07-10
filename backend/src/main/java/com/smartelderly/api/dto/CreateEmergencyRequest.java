package com.smartelderly.api.dto;

import com.smartelderly.domain.AlertType;
import com.smartelderly.domain.TriggerMode;

import jakarta.validation.constraints.NotNull;

public record CreateEmergencyRequest(
        @NotNull(message = "alertType required") AlertType alertType,
        TriggerMode triggerMode,
        Long locationId,
        String remark) {
}
