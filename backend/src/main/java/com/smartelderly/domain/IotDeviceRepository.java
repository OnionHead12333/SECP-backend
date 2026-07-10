package com.smartelderly.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {

    Optional<IotDevice> findByDeviceId(String deviceId);

    List<IotDevice> findByFamilyIdInOrderByUpdatedAtDesc(List<Long> familyIds);
}
