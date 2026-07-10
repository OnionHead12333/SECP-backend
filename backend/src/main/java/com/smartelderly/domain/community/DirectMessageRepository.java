package com.smartelderly.domain.community;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, String> {

    List<DirectMessage> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    Optional<DirectMessage> findByIdAndThreadId(String id, Long threadId);
}