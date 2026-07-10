package com.smartelderly.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestCommunityMembershipRepository extends JpaRepository<InterestCommunityMembership, Long> {

    List<InterestCommunityMembership> findByElderProfileIdAndStatus(Long elderProfileId, String status);

    Optional<InterestCommunityMembership> findByElderProfileIdAndCommunityId(Long elderProfileId, String communityId);
}
