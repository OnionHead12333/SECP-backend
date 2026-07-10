package com.smartelderly.domain.community;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectMessageClearRepository extends JpaRepository<DirectMessageClear, Long> {
    Optional<DirectMessageClear> findByThreadIdAndScopeKey(Long threadId, String scopeKey);
}
