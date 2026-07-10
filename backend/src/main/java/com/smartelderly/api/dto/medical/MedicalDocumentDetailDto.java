package com.smartelderly.api.dto.medical;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.smartelderly.api.dto.medical.MedicalDisplayBlockDto;
import com.smartelderly.api.dto.MedicalOcrView;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalDocumentDetailDto {
    private Long id;
    private Long elderProfileId;
    private Long folderId;
    private String folderName;
    private String title;
    private String originalFilename;
    private String docCategory;
    private String routedSpecializedApi;
    private String structuredRouteSource;
    private String structuredError;
    private String fullText;
    private ExtractedMedicalFieldsDto extractedFields;
    private MedicalOcrView ocr;
    private List<MedicalDisplayBlockDto> displayBlocks;
    private Map<String, String> structuredFields;
    private LocalDateTime createdAt;
}
