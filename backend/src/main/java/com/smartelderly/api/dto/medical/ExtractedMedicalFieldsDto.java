package com.smartelderly.api.dto.medical;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** OCR 全文启发式抽取结果（非百度结构化字段补全）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedMedicalFieldsDto {
    /** 推断单据大类：LAB_REPORT / PRESCRIPTION / INVOICE / STATEMENT / OTHER */
    private String docCategory;

    private List<String> detectedDateTexts = new ArrayList<>();
    /** yyyy-MM-dd */
    private List<String> normalizedDates = new ArrayList<>();

    private List<String> matchedKeywords = new ArrayList<>();
}
