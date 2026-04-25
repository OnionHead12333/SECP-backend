package com.smartelderly.domain.location;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElderLocationGuardSettingRepository extends JpaRepository<ElderLocationGuardSetting, Long> {

    Optional<ElderLocationGuardSetting> findByElderProfileId(Long elderProfileId);
}
