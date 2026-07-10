package com.smartelderly.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestCommunityChatClearRepository extends JpaRepository<InterestCommunityChatClear, Long> {

    Optional<InterestCommunityChatClear> findByViewerScopeKeyAndCommunityId(String viewerScopeKey, String communityId);
}
