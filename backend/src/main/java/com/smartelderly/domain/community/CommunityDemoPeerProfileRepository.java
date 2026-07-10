package com.smartelderly.domain.community;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityDemoPeerProfileRepository extends JpaRepository<CommunityDemoPeerProfile, String> {

    Optional<CommunityDemoPeerProfile> findByPhone(String phone);

    List<CommunityDemoPeerProfile> findAllByOrderByCreatedAtAsc();
}