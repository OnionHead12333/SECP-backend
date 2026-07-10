package com.smartelderly.domain.community;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElderFriendRepository extends JpaRepository<ElderFriend, Long> {

    List<ElderFriend> findByOwnerElderProfileIdOrderByAddedAtDesc(Long ownerElderProfileId);

    Optional<ElderFriend> findByOwnerElderProfileIdAndFriendScopeKey(Long ownerElderProfileId, String friendScopeKey);

    Optional<ElderFriend> findByOwnerElderProfileIdAndPhone(Long ownerElderProfileId, String phone);

    boolean existsByOwnerElderProfileIdAndFriendScopeKey(Long ownerElderProfileId, String friendScopeKey);

    boolean existsByOwnerElderProfileIdAndPhone(Long ownerElderProfileId, String phone);
}