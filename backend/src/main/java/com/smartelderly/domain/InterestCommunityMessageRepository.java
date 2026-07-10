package com.smartelderly.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestCommunityMessageRepository extends JpaRepository<InterestCommunityMessage, String> {

    List<InterestCommunityMessage> findByCommunityIdOrderByCreatedAtAscIdAsc(String communityId);

    java.util.Optional<InterestCommunityMessage> findByIdAndCommunityId(String id, String communityId);
}
