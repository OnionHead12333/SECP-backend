package com.smartelderly.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.ElderUserProfileView;
import com.smartelderly.api.dto.UpdateElderUserProfileRequest;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.User;
import com.smartelderly.domain.UserRepository;

@Service
public class ElderUserProfileService {

    private final UserRepository userRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final FamilyBindingRepository familyBindingRepository;

    public ElderUserProfileService(
            UserRepository userRepository,
            ElderProfileRepository elderProfileRepository,
            FamilyBindingRepository familyBindingRepository) {
        this.userRepository = userRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.familyBindingRepository = familyBindingRepository;
    }

    @Transactional(readOnly = true)
    public ElderUserProfileView getProfileForElderUser(long elderUserId) {
        User user = userRepository.findById(elderUserId)
                .orElseThrow(() -> new ApiException(4010, "unauthorized"));
        if (!"elder".equalsIgnoreCase(user.getRole())) {
            throw new ApiException(4030, "forbidden");
        }
        return buildView(user);
    }

    @Transactional
    public ElderUserProfileView updateProfile(long elderUserId, UpdateElderUserProfileRequest request) {
        User user = userRepository.findById(elderUserId)
                .orElseThrow(() -> new ApiException(4010, "unauthorized"));
        if (!"elder".equalsIgnoreCase(user.getRole())) {
            throw new ApiException(4030, "forbidden");
        }

        String name = request.getName().trim();
        if (name.isEmpty()) {
            throw new ApiException(400, "姓名不能为空");
        }
        user.setName(name);
        user.setGender(request.getGender());
        if (request.getBirthday() != null && request.getBirthday().isAfter(LocalDate.now())) {
            throw new ApiException(400, "出生日期不能是未来日期");
        }
        user.setBirthday(request.getBirthday());
        userRepository.save(user);

        syncElderProfile(user);

        return buildView(user);
    }

    private void syncElderProfile(User user) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return;
        }
        String phone = user.getPhone().trim();
        ElderProfile profile = elderProfileRepository.findByClaimedUserId(user.getId())
                .or(() -> elderProfileRepository.findByPhone(phone))
                .orElse(null);
        if (profile == null) {
            return;
        }
        if (profile.getClaimedUserId() != null && !profile.getClaimedUserId().equals(user.getId())) {
            return;
        }
        profile.setName(user.getName());
        profile.setGender(user.getGender());
        profile.setBirthday(user.getBirthday());
        elderProfileRepository.save(profile);
    }

    private ElderUserProfileView buildView(User user) {
        boolean claimed = true;
        int familyCount = 0;
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            var profOpt = elderProfileRepository.findByPhone(user.getPhone().trim());
            if (profOpt.isPresent()) {
                var ep = profOpt.get();
                claimed = ep.getClaimedUserId() != null;
                familyCount = familyBindingRepository
                        .findByElderProfileIdAndStatus(ep.getId(), BindingStatus.active)
                        .size();
            }
        }
        return ElderUserProfileView.builder()
                .name(user.getName())
                .phone(user.getPhone())
                .gender(user.getGender() != null ? user.getGender() : "unknown")
                .birthday(user.getBirthday() != null ? user.getBirthday().toString() : null)
                .claimed(claimed)
                .familyCount(familyCount)
                .build();
    }
}
