package com.smartelderly.api.dto;

import jakarta.validation.constraints.NotBlank;

public record HandleEmergencyRequest(
        @NotBlank(message = "action required") String action,
        String remark) {
}
