package com.smartelderly.domain.emergency_contact;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    // 查询某个老人的所有紧急联系人，按优先级升序排列
    List<EmergencyContact> findByElderProfileIdOrderByPriorityAsc(Long elderProfileId);

    // 查询某个老人的所有紧急联系人，按优先级升序，再按ID升序排列
    List<EmergencyContact> findByElderProfileIdOrderByPriorityAscIdAsc(Long elderProfileId);

    // 检查是否存在相同的phone
    boolean existsByElderProfileIdAndPhone(Long elderProfileId, String phone);
}
