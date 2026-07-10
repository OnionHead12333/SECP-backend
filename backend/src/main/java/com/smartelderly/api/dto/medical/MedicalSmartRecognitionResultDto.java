package com.smartelderly.api.dto.medical;

import java.util.List;
import java.util.Map;

import com.smartelderly.api.dto.MedicalOcrView;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalSmartRecognitionResultDto {
    private Long documentId;
    private MedicalOcrView ocr;
    private ExtractedMedicalFieldsDto extractedFields;
    private List<SuggestedCalendarEventDto> suggestedCalendarEvents;
    private List<MedicalDisplayBlockDto> displayBlocks;
    private Map<String, String> structuredFields;
    private String structuredError;
    /** 可选变体：例如 tableRows、specializedRaw、rawOcrWords 等，供前端切换显示 */
    private Map<String, Object> variants;
}
