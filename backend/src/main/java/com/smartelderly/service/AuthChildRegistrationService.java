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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * 注册子女账号，并为每位老人建立档案或复用已有档案，再建立绑定关系（不重复绑定、状态可绑定时才建绑）。
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

        Set<String> distinctElderPhones = new HashSet<>();
        for (RegisterChildWithEldersRequest.ElderItem e : elders) {
            if (!distinctElderPhones.add(e.getPhone().trim())) {
                return ApiResponse.error(400, "老人列表中存在重复手机号");
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
            String elderPhone = e.getPhone().trim();
            ElderProfile profile = resolveOrCreateElderProfile(e, elderPhone, savedChild.getId());
            if (!isElderProfileBindable(profile)) {
                return ApiResponse.error(400, "老人档案（" + elderPhone + "）当前状态不可绑定");
            }
            if (familyBindingRepository.existsByChildUserIdAndElderProfileId(savedChild.getId(), profile.getId())) {
                continue;
            }
            FamilyBinding binding = new FamilyBinding();
            binding.setElderProfileId(profile.getId());
            binding.setChildUserId(savedChild.getId());
            binding.setRelation(e.getRelation().trim());
            binding.setIsPrimary(i == 0);
            binding.setStatus(BindingStatus.active);
            familyBindingRepository.save(binding);
        }

        int familyCount = familyBindingRepository
                .findByChildUserIdAndStatus(savedChild.getId(), BindingStatus.active)
                .size();
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
                .familyCount(familyCount)
                .build();
        return ApiResponse.success("注册成功", body);
    }

    /**
     * 不存在则新建老人档案；已存在则复用，不覆盖姓名等字段。
     */
    private ElderProfile resolveOrCreateElderProfile(
            RegisterChildWithEldersRequest.ElderItem e, String elderPhone, Long childUserId) {
        return elderProfileRepository
                .findByPhone(elderPhone)
                .orElseGet(() -> {
                    ElderProfile p = new ElderProfile();
                    p.setName(e.getName().trim());
                    p.setPhone(elderPhone);
                    p.setCreatedByChildId(childUserId);
                    p.setStatus("unclaimed");
                    p.setLocationPermissionForeground(false);
                    p.setLocationPermissionBackground(false);
                    return elderProfileRepository.save(p);
                });
    }

    /**
     * 业务上「状态正常」的档案才允许与当前子女建立新绑定（可与其他子女并存）。
     */
    private static boolean isElderProfileBindable(ElderProfile profile) {
        String s = profile.getStatus();
        if (s == null || s.isBlank()) {
            return true;
        }
        return "unclaimed".equals(s) || "claimed".equals(s) || "normal".equals(s);
    }
}
