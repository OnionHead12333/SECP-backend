package com.smartelderly.api.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiKnowledgeImportResponse {

    private Long jobId;
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private Integer duplicateRows;
    private String importStatus;
}
