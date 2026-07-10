package com.smartelderly.domain.community;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageThreadRepository extends JpaRepository<DirectMessageThread, Long> {

    Optional<DirectMessageThread> findByParticipantAScopeKeyAndParticipantBScopeKey(
            String participantAScopeKey, String participantBScopeKey);
}