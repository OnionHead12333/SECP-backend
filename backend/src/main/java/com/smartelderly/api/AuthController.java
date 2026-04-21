package com.smartelderly.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.AuthResponse;
import com.smartelderly.api.dto.LoginRequest;
import com.smartelderly.api.dto.RegisterRequest;

/**
 * 认证授权控制器 (V1)
 * 遵循《注册绑定流程设计文档》与《老人端注册登录接口设计》
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    /**
     * 用户登录接口
     * 支持老人和子女通过同一接口登录
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 模拟简单的账号密码校验逻辑
        if ("13800138001".equals(loginRequest.getUsername()) && "123456".equals(loginRequest.getPassword())) {
            AuthResponse mockResponse = AuthResponse.builder()
                    .token("jwt-token-demo-" + System.currentTimeMillis())
                    .userId(1001L)
                    .role("elder")
                    .username(loginRequest.getUsername())
                    .name("张建国")
                    .phone(loginRequest.getUsername())
                    .nickname("建国叔")
                    .claimed(true)
                    .familyCount(1)
                    .build();
            return ApiResponse.success("登录成功", mockResponse);
        } else {
            // 失败时返回统一的错误结构，避免 Apifox 校验 data 字段失败
            return ApiResponse.error(401, "用户名或密码错误");
        }
    }

    /**
     * 用户注册接口
     * 通过 role 字段区分 1.子女注册 (child) 2.老人注册 (elder)
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // TODO: 实现注册逻辑
        // 1. 检查手机号/用户名是否已存在
        // 2. 根据 role 执行不同的业务流程 (子女建主体/申请绑定 vs 老人认领/独立注册)
        
        AuthResponse mockResult = AuthResponse.builder()
                .userId(1001L)
                .role(registerRequest.getRole())
                .username(registerRequest.getUsername())
                .phone(registerRequest.getPhone())
                .name(registerRequest.getName())
                .nickname(registerRequest.getNickname() != null ? registerRequest.getNickname() : registerRequest.getName())
                .claimed(false)
                .familyCount(0)
                .build();

        return ApiResponse.success("注册成功", mockResult);
    }
}
