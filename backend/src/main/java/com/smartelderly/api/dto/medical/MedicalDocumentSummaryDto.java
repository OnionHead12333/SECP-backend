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
public class MedicalDocumentSummaryDto {
    private Long id;
    private Long elderProfileId;
    private String title;
    private String docCategory;
    private String routedSpecializedApi;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
}
