package com.smartelderly.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyBindingRepository extends JpaRepository<FamilyBinding, Long> {

    List<FamilyBinding> findByChildUserIdAndStatus(Long childUserId, BindingStatus status);
}
