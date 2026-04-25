package com.smartelderly.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.dto.ElderBoundChildResponse;
import com.smartelderly.api.ApiException;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.FamilyBinding;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.User;
import com.smartelderly.domain.UserRepository;

@Service
public class ElderBoundFamilyService {

    private final UserRepository userRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final FamilyBindingRepository familyBindingRepository;

    public ElderBoundFamilyService(
            UserRepository userRepository,
            ElderProfileRepository elderProfileRepository,
            FamilyBindingRepository familyBindingRepository) {
        this.userRepository = userRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.familyBindingRepository = familyBindingRepository;
    }

    /**
     * 当前老人登录账号（手机号）对应档案在 {@code family_bindings} 中的子女端用户。
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<ElderBoundChildResponse>> listBoundChildrenForElderUser(long elderUserId) {
        User elderUser = userRepository.findById(elderUserId)
                .orElseThrow(() -> new ApiException(4010, "unauthorized"));
        if (!"elder".equalsIgnoreCase(elderUser.getRole())) {
            throw new ApiException(4030, "forbidden");
        }
        if (elderUser.getPhone() == null || elderUser.getPhone().isBlank()) {
            return ApiResponse.ok(List.of());
        }
        ElderProfile profile = elderProfileRepository.findByPhone(elderUser.getPhone().trim())
                .orElse(null);
        if (profile == null) {
            return ApiResponse.ok(List.of());
        }
        List<FamilyBinding> bindings = familyBindingRepository.findByElderProfileIdAndStatus(
                profile.getId(), BindingStatus.active);
        List<ElderBoundChildResponse> out = new ArrayList<>();
        for (FamilyBinding b : bindings) {
            userRepository.findById(b.getChildUserId()).ifPresent(child -> {
                if (!"child".equalsIgnoreCase(child.getRole())) {
                    return;
                }
                String rel = b.getRelation() != null && !b.getRelation().isBlank() ? b.getRelation() : "家人";
                out.add(new ElderBoundChildResponse(
                        child.getId(),
                        child.getName() != null ? child.getName() : "",
                        child.getPhone() != null ? child.getPhone() : "",
                        rel,
                        Boolean.TRUE.equals(b.getIsPrimary())));
            });
        }
        return ApiResponse.ok(out);
    }
}
