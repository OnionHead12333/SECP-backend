package com.smartelderly.api.child;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ChildFallAlert(
        Long id,
        String type,
        String title,
        String message,
        String description,
        Long elderProfileId,
        String elderName,
        String identitySource,
        Double identityConfidence,
        Boolean notifiedChild,
        String locationName,
        Double x,
        Double y,
        String level,
        String status,
        String imageUrl,
        String time,
        String handler,
        String remark,
        String handleTime) {
}
