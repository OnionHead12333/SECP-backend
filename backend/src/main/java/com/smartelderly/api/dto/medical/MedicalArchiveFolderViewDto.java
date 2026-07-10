package com.smartelderly.api.dto.medical;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalArchiveFolderViewDto {
    private Long id;
    private Long elderProfileId;
    private String name;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
