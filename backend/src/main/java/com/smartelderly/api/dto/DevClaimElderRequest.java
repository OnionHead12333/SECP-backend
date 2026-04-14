package com.smartelderly.api.dto;

import jakarta.validation.constraints.NotNull;

public record DevClaimElderRequest(
        @NotNull Long elderProfileId,
        @NotNull Long claimedUserId) {
}

