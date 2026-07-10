package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiKnowledgeImportJobResponse {

    private Long id;
    private String fileName;
    private String filePath;
    private String importStatus;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private String errorMessage;
}
