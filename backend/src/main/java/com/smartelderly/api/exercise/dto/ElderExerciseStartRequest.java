package com.smartelderly.api.exercise.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ElderExerciseStartRequest {

    @NotNull
    private Long elderId;

    @NotNull
    private String startedAt;
}
