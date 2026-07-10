package com.smartelderly.api.dto.medical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalDisplayBlockDto {
    /** title / kv / note / paragraph */
    private String type;
    private String text;
    private String label;
    private String value;
}