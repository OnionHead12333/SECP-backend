package com.smartelderly.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.ElderBoundChildResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.ElderBoundFamilyService;

@RestController
@RequestMapping("/v1/elder")
public class ElderBoundFamilyController {

    private final ElderBoundFamilyService elderBoundFamilyService;

    public ElderBoundFamilyController(ElderBoundFamilyService elderBoundFamilyService) {
        this.elderBoundFamilyService = elderBoundFamilyService;
    }

    /**
     * 当前老人账号下，家庭绑定表中的子女列表。
     */
    @GetMapping("/bound-children")
    public ApiResponse<List<ElderBoundChildResponse>> listBoundChildren() {
        var p = SecurityUtils.requireRole(UserRole.elder);
        return elderBoundFamilyService.listBoundChildrenForElderUser(p.userId());
    }
}
