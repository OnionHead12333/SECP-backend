package com.smartelderly.api.dto.medical;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@Data
public class PatchMedicalDocumentRequest {
    private String title;
    private String fullText;
    private String docCategory;
    private Long folderId;

    @JsonIgnore
    private boolean folderIdSpecified;

    @JsonSetter("folderId")
    public void setFolderId(Long folderId) {
        this.folderId = folderId;
        this.folderIdSpecified = true;
    }
}
