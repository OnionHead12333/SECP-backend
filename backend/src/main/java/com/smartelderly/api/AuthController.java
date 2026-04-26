package com.smartelderly.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.AuthResponse;
import com.smartelderly.api.dto.LoginRequest;
import com.smartelderly.api.dto.RegisterRequest;
import com.smartelderly.api.dto.RegisterChildWithEldersRequest;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.User;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.JwtService;
import com.smartelderly.service.AuthChildRegistrationService;
import com.smartelderly.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 认证授权控制器 (V1)
 * 遵循《注册绑定流程设计文档》与《老人端注册登录接口设计》
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthChildRegistrationService authChildRegistrationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ElderProfileRepository elderProfileRepository;

    @Autowired
    private FamilyBindingRepository familyBindingRepository;

    /**
     * 用户登录接口
     * 支持老人和子女通过同一接口登录
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 1. 查找用户
        User user = userService.findByUsername(loginRequest.getUsername()).orElse(null);
        if (user == null) {
            user = userService.findByPhone(loginRequest.getUsername()).orElse(null);
        }
        if (user == null) {
            return ApiResponse.error(401, "用户不存在");
        }
        // 2. 校验密码
        if (!userService.checkPassword(user, loginRequest.getPassword())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        // 3. 签发与 JwtAuthenticationFilter 一致的标准 HS256 JWT（JWS 含两个英文句点，共三段）
        UserRole role;
        try {
            role = UserRole.valueOf(user.getRole());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(500, "用户角色数据异常");
        }
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        String token = jwtService.issueToken(user.getId(), role, expiresAt);
        boolean claimed = true;
        int familyCount = 0;
        if ("elder".equalsIgnoreCase(user.getRole()) && user.getPhone() != null && !user.getPhone().isBlank()) {
            var profOpt = elderProfileRepository.findByPhone(user.getPhone().trim());
            if (profOpt.isPresent()) {
                var ep = profOpt.get();
                claimed = ep.getClaimedUserId() != null;
                familyCount = familyBindingRepository
                        .findByElderProfileIdAndStatus(ep.getId(), BindingStatus.active)
                        .size();
            }
        } else if ("child".equalsIgnoreCase(user.getRole())) {
            familyCount = familyBindingRepository
                    .findByChildUserIdAndStatus(user.getId(), BindingStatus.active)
                    .size();
        }
        AuthResponse resp = AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole())
                .username(user.getUsername())
                .name(user.getName())
                .phone(user.getPhone())
                .nickname(user.getName())
                .gender(user.getGender())
                .birthday(user.getBirthday() != null ? user.getBirthday().toString() : null)
                .claimed(claimed)
                .familyCount(familyCount)
                .build();
        return ApiResponse.success("登录成功", resp);
    }

    /**
     * 用户注册接口
     * 通过 role 字段区分 1.子女注册 (child) 2.老人注册 (elder)
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // 1. 检查用户名/手机号是否已存在
        if (userService.findByUsername(registerRequest.getUsername()).isPresent()) {
            return ApiResponse.error(400, "用户名已存在");
        }
        if (registerRequest.getPhone() != null && userService.findByPhone(registerRequest.getPhone()).isPresent()) {
            return ApiResponse.error(400, "手机号已存在");
        }
        // 2. 创建用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setRole(registerRequest.getRole());
        user.setName(registerRequest.getName());
        user.setPhone(registerRequest.getPhone());
        User saved = userService.register(user, registerRequest.getPassword());

        boolean claimed = false;
        int familyCount = 0;
        if ("elder".equalsIgnoreCase(saved.getRole())) {
            ElderProfile elderProfile = ensureElderProfileForRegisteredUser(saved, registerRequest);
            claimed = elderProfile.getClaimedUserId() != null;
        }

        // 3. 构造响应
        AuthResponse resp = AuthResponse.builder()
                .userId(saved.getId())
                .role(saved.getRole())
                .username(saved.getUsername())
                .phone(saved.getPhone())
                .name(saved.getName())
                .nickname(saved.getName())
                .gender(saved.getGender())
                .birthday(saved.getBirthday() != null ? saved.getBirthday().toString() : null)
                .claimed(claimed)
                .familyCount(familyCount)
                .build();
        return ApiResponse.success("注册成功", resp);
    }

    private ElderProfile ensureElderProfileForRegisteredUser(User saved, RegisterRequest registerRequest) {
        var existing = elderProfileRepository.findByPhone(registerRequest.getPhone()).orElse(null);
        if (existing != null) {
            Long claimedUserId = existing.getClaimedUserId();
            if (claimedUserId != null && !claimedUserId.equals(saved.getId())) {
                throw new ApiException(4002, "该老人主体已被认领");
            }
            existing.setName(registerRequest.getName());
            existing.setClaimedUserId(saved.getId());
            existing.setStatus("claimed");
            return elderProfileRepository.save(existing);
        }

        ElderProfile profile = new ElderProfile();
        profile.setName(registerRequest.getName());
        profile.setPhone(registerRequest.getPhone());
        profile.setClaimedUserId(saved.getId());
        profile.setCreatedByChildId(null);
        profile.setStatus("claimed");
        return elderProfileRepository.save(profile);
    }

    /**
     * 子女注册并绑定老人主体（与移动端多步表单对应）。
     */
    @PostMapping("/register-child-with-elders")
    public ApiResponse<AuthResponse> registerChildWithElders(
            @Valid @RequestBody RegisterChildWithEldersRequest request) {
        return authChildRegistrationService.registerChildWithElders(request);
    }
}
