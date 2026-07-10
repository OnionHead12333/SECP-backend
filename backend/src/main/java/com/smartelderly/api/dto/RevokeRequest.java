package com.smartelderly.api.dto;

import com.smartelderly.domain.CancelMode;

import jakarta.validation.constraints.NotNull;

public record RevokeRequest(@NotNull(message = "cancelMode required") CancelMode cancelMode) {
}
