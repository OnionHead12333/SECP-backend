package com.smartelderly.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.ChildBoundElderResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.ChildBoundElderService;

@RestController
@RequestMapping("/v1/child")
public class ChildBoundElderController {

    private final ChildBoundElderService childBoundElderService;

    public ChildBoundElderController(ChildBoundElderService childBoundElderService) {
        this.childBoundElderService = childBoundElderService;
    }

    /**
     * 当前子女账号在 {@code family_bindings} 中已激活绑定的老人列表（不含求助记录、不含本机手填）。
     */
    @GetMapping("/bound-elders")
    public ApiResponse<List<ChildBoundElderResponse>> listBoundElders() {
        var p = SecurityUtils.requireRole(UserRole.child);
        return childBoundElderService.listBoundElders(p.userId());
    }
}
