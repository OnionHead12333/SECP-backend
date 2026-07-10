package com.smartelderly.domain.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// ElderGuardRuleRepository接口，提供根据elderProfileId查询监护规则的方法
public interface ElderGuardRuleRepository extends JpaRepository<ElderGuardRule, Long> {
    Optional<ElderGuardRule> findByElderProfileId(Long elderProfileId);

    /**
     * 查询所有启用的监护规则
     */
    List<ElderGuardRule> findByEnabled(Boolean enabled);
}
// 这个接口继承了JpaRepository，提供了基本的CRUD操作，save（）方法自带。