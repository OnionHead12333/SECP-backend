package com.smartelderly.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    Optional<FamilyMember> findFirstByFamilyIdAndPrimaryTrueOrderByIdAsc(Long familyId);

    List<FamilyMember> findByFamilyIdOrderByIdAsc(Long familyId);

    List<FamilyMember> findByElderProfileIdOrderByIdAsc(Long elderProfileId);
}
