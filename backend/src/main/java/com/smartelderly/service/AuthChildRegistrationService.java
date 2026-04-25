package com.smartelderly.service;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.AuthResponse;
import com.smartelderly.api.dto.RegisterChildWithEldersRequest;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.FamilyBinding;
import com.smartelderly.domain.User;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.FamilyBindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthChildRegistrationService {

    private final UserService userService;
    private final ElderProfileRepository elderProfileRepository;
    private final FamilyBindingRepository familyBindingRepository;

    public AuthChildRegistrationService(
            UserService userService,
            ElderProfileRepository elderProfileRepository,
            FamilyBindingRepository familyBindingRepository) {
        this.userService = userService;
        this.elderProfileRepository = elderProfileRepository;
        this.familyBindingRepository = familyBindingRepository;
    }

    /**
     * 注册子女账号，并为每位老人建立档案 + 与子女的绑定关系。
     */
    @Transactional
    public ApiResponse<AuthResponse> registerChildWithElders(RegisterChildWithEldersRequest request) {
        var child = request.getChild();
        String childPhone = child.getPhone().trim();
        if (userService.findByUsername(childPhone).isPresent()) {
            return ApiResponse.error(400, "该手机号已注册");
        }
        if (userService.findByPhone(childPhone).isPresent()) {
            return ApiResponse.error(400, "该手机号已注册");
        }

        List<RegisterChildWithEldersRequest.ElderItem> elders = request.getElders();
        for (RegisterChildWithEldersRequest.ElderItem e : elders) {
            if (e.getPhone().trim().equals(childPhone)) {
                return ApiResponse.error(400, "老人手机号不能与子女手机号相同");
            }
        }

        for (RegisterChildWithEldersRequest.ElderItem e : elders) {
            if (elderProfileRepository.findByPhone(e.getPhone().trim()).isPresent()) {
                return ApiResponse.error(400, "老人手机号 " + e.getPhone() + " 已存在档案");
            }
        }

        User user = new User();
        user.setUsername(childPhone);
        user.setRole("child");
        String displayName = child.getName().trim();
        user.setName(displayName);
        user.setPhone(childPhone);
        User savedChild = userService.register(user, child.getPassword());

        for (int i = 0; i < elders.size(); i++) {
            RegisterChildWithEldersRequest.ElderItem e = elders.get(i);
            ElderProfile profile = new ElderProfile();
            profile.setName(e.getName().trim());
            profile.setPhone(e.getPhone().trim());
            profile.setCreatedByChildId(savedChild.getId());
            profile.setStatus("unclaimed");
            profile.setLocationPermissionForeground(false);
            profile.setLocationPermissionBackground(false);
            ElderProfile savedProfile = elderProfileRepository.save(profile);

            FamilyBinding binding = new FamilyBinding();
            binding.setElderProfileId(savedProfile.getId());
            binding.setChildUserId(savedChild.getId());
            binding.setRelation(e.getRelation().trim());
            binding.setIsPrimary(i == 0);
            binding.setStatus(BindingStatus.active);
            familyBindingRepository.save(binding);
        }

        String nickname = child.getNickname() != null && !child.getNickname().isBlank()
                ? child.getNickname().trim()
                : displayName;
        AuthResponse body = AuthResponse.builder()
                .userId(savedChild.getId())
                .role("child")
                .username(savedChild.getUsername())
                .phone(savedChild.getPhone())
                .name(savedChild.getName())
                .nickname(nickname)
                .claimed(false)
                .familyCount(elders.size())
                .build();
        return ApiResponse.success("注册成功", body);
    }
}
