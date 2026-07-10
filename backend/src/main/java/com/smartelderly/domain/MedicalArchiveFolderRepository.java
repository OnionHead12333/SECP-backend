package com.smartelderly.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalArchiveFolderRepository extends JpaRepository<MedicalArchiveFolder, Long> {

    List<MedicalArchiveFolder> findByElderProfileIdOrderBySortOrderAscIdAsc(Long elderProfileId);

    java.util.Optional<MedicalArchiveFolder> findByIdAndElderProfileId(Long id, Long elderProfileId);

    boolean existsByElderProfileIdAndName(Long elderProfileId, String name);
}
