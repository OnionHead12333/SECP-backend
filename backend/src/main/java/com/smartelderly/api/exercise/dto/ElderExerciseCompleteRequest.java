package com.smartelderly.api.exercise.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ElderExerciseCompleteRequest {

    @NotNull
    private Long elderId;

    @NotNull
    private String confirmedAt;

    /** manual / sensor */
    private String source;
}
