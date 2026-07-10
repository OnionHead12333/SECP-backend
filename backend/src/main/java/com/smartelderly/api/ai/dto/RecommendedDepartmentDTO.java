package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendedDepartmentDTO {

    private Long departmentId;
    private String departmentName;
    private String reason;
}
