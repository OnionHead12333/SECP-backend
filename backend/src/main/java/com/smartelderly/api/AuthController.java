package com.smartelderly.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.AuthResponse;
import com.smartelderly.api.dto.LoginRequest;
import com.smartelderly.api.dto.RegisterRequest;
import com.smartelderly.domain.User;
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
        // 3. 构造响应
        AuthResponse resp = AuthResponse.builder()
                .token("jwt-token-demo-" + System.currentTimeMillis()) // TODO: 替换为真实 JWT
                .userId(user.getId())
                .role(user.getRole())
                .username(user.getUsername())
                .name(user.getName())
                .phone(user.getPhone())
                .nickname(user.getName())
                .claimed(true)
                .familyCount(0)
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
        // 密码加密存储
        User saved = userService.register(user, registerRequest.getPassword());
        // 3. 构造响应
        AuthResponse resp = AuthResponse.builder()
                .userId(saved.getId())
                .role(saved.getRole())
                .username(saved.getUsername())
                .phone(saved.getPhone())
                .name(saved.getName())
                .nickname(saved.getName())
                .claimed(false)
                .familyCount(0)
                .build();
        return ApiResponse.success("注册成功", resp);
    }
}
