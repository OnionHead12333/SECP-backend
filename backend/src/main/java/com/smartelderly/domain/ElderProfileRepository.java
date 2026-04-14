package com.smartelderly.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElderProfileRepository extends JpaRepository<ElderProfile, Long> {

    Optional<ElderProfile> findByClaimedUserId(Long claimedUserId);
}
