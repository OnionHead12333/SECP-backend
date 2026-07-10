package com.smartelderly.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestCommunityRepository extends JpaRepository<InterestCommunity, String> {

    List<InterestCommunity> findByIsActiveTrueOrderBySortOrderAsc();
}
