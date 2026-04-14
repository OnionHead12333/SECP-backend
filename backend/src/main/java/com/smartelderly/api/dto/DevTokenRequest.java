package com.smartelderly.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DevTokenRequest(
        @NotNull Long userId,
        @NotBlank String role,
        @Min(60) @Max(86400) Integer ttlSeconds) {
}

