package com.smartelderly.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {

    List<MedicalDocument> findByElderProfileIdOrderByCreatedAtDesc(Long elderProfileId);

    List<MedicalDocument> findByElderProfileIdAndFolderIdOrderByCreatedAtDesc(
            Long elderProfileId, Long folderId);

    java.util.Optional<MedicalDocument> findByIdAndElderProfileId(Long id, Long elderProfileId);

    List<MedicalDocument> findByElderProfileIdAndDocCategoryOrderByCreatedAtDesc(
            Long elderProfileId, String docCategory);
}
