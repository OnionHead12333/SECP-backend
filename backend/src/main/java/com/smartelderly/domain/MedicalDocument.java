package com.smartelderly.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "medical_documents")
public class MedicalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "elder_profile_id", nullable = false)
    private Long elderProfileId;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    @Column(name = "folder_id")
    private Long folderId;

    @Column(length = 256)
    private String title;

    @Column(name = "original_filename", length = 512)
    private String originalFilename;

    @Column(name = "stored_path", nullable = false, length = 1024)
    private String storedPath;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "doc_category", length = 64)
    private String docCategory;

    @Column(name = "routed_specialized_api", length = 64)
    private String routedSpecializedApi;

    @Column(name = "structured_route_source", length = 32)
    private String structuredRouteSource;

    @Column(name = "full_text", columnDefinition = "MEDIUMTEXT")
    private String fullText;

    @Column(name = "ocr_raw_json", columnDefinition = "LONGTEXT")
    private String ocrRawJson;

    @Column(name = "classify_raw_json", columnDefinition = "LONGTEXT")
    private String classifyRawJson;

    @Column(name = "specialized_raw_json", columnDefinition = "LONGTEXT")
    private String specializedRawJson;

    @Column(name = "display_blocks_json", columnDefinition = "LONGTEXT")
    private String displayBlocksJson;

    @Column(name = "structured_fields_json", columnDefinition = "LONGTEXT")
    private String structuredFieldsJson;

    @Column(name = "structured_error", length = 512)
    private String structuredError;

    @Column(name = "extracted_fields_json", columnDefinition = "LONGTEXT")
    private String extractedFieldsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
