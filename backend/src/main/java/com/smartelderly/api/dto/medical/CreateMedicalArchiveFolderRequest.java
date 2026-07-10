package com.smartelderly.api.dto.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMedicalArchiveFolderRequest {
    @NotNull private Long elderProfileId;

    @NotBlank private String name;
}
