package com.smartelderly.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 老人端：与当前老人在 {@code family_bindings} 中已激活的子女用户。
 */
public record ElderBoundChildResponse(
        @JsonProperty("childUserId") long childUserId,
        String name,
        String phone,
        String relation,
        @JsonProperty("isPrimary") boolean isPrimary) {
}
