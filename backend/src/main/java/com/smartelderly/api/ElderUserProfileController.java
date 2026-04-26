package com.smartelderly.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.ElderUserProfileView;
import com.smartelderly.api.dto.UpdateElderUserProfileRequest;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.ElderUserProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/elder/profile")
public class ElderUserProfileController {

    private final ElderUserProfileService elderUserProfileService;

    public ElderUserProfileController(ElderUserProfileService elderUserProfileService) {
        this.elderUserProfileService = elderUserProfileService;
    }

    @GetMapping
    public ApiResponse<ElderUserProfileView> getProfile() {
        var p = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.ok(elderUserProfileService.getProfileForElderUser(p.userId()));
    }

    @PatchMapping
    public ApiResponse<ElderUserProfileView> updateProfile(@Valid @RequestBody UpdateElderUserProfileRequest request) {
        var p = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.ok("ok", elderUserProfileService.updateProfile(p.userId(), request));
    }
}
