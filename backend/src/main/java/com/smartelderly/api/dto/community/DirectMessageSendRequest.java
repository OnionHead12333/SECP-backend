package com.smartelderly.api.dto.community;

import jakarta.validation.constraints.NotBlank;

public record DirectMessageSendRequest(
        @NotBlank(message = "kind required") String kind,
        String textContent) {
}