package com.smartelderly.api.inspection;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record InspectionApiResponse<T>(
        boolean success,
        String message,
        T data) {

    public static <T> InspectionApiResponse<T> ok(T data) {
        return new InspectionApiResponse<>(true, "ok", data);
    }

    public static <T> InspectionApiResponse<T> fail(String message) {
        return new InspectionApiResponse<>(false, message, null);
    }
}
