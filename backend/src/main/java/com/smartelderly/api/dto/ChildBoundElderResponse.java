package com.smartelderly.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 子女端：当前登录子女与家庭绑定表中的老人条目。
 */
public record ChildBoundElderResponse(
        @JsonProperty("elderProfileId") long elderProfileId,
        String name,
        String phone,
        String relation,
        @JsonProperty("isPrimary") boolean isPrimary) {
}
