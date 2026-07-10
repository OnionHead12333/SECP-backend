package com.smartelderly.api.ai.dto;

import lombok.Data;

@Data
public class AiKnowledgeImportRequest {

    private String filePath;
    private String fileType;
    private Boolean clearBeforeImport;
}
