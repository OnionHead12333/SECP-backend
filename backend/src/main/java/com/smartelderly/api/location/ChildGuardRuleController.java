package com.smartelderly.api.location;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.GuardRuleResponse;
import com.smartelderly.api.location.dto.SaveGuardRuleRequest;
import com.smartelderly.api.location.dto.SaveGuardRuleResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.location.GuardRuleService;

import jakarta.validation.Valid;


@RestController//告诉 Spring 这是一个控制器，且方法的返回值会自动转换成 JSON 格式发送给前端
@RequestMapping("/v1/child/elders")//设置这个控制器的基础 URL 路径，这个类下所有方法的 URL 都会以这个路径开头
public class ChildGuardRuleController {

    private final GuardRuleService guardRuleService;

    // 构造函数，使用构造函数注入 GuardRuleService(准备好Service，前端调用时调用Service的方法获取数据)
    public ChildGuardRuleController(GuardRuleService guardRuleService) {
        this.guardRuleService = guardRuleService;
    }

    /**
     * 获取监护规则
     * @param elderId 老人ID
     * @return 监护规则信息
     */
    @GetMapping("/{elderId}/guard-rule")
    public ApiResponse<GuardRuleResponse> getGuardRule(@PathVariable("elderId") Long elderId) {
        // 鉴权：验证当前登录用户的角色必须是child，否则抛出403异常
        var user = SecurityUtils.requireRole(UserRole.child);
        // 调用Service获取监护规则信息，并返回给前端（Service处理逻辑并调用ApiResponse）
        return guardRuleService.getGuardRule(elderId);
    }

    /**
     * 保存监护规则
     * @param elderId 老人ID
     * @param request 保存请求
     * @return 保存结果
     */
    @PutMapping("/{elderId}/guard-rule")
    public ApiResponse<SaveGuardRuleResponse> saveGuardRule(
            @PathVariable("elderId") Long elderId,
            // 使用@Valid启用请求体的验证，使用@RequestBody将请求体转换成SaveGuardRuleRequest对象
            @Valid @RequestBody SaveGuardRuleRequest request) {
        // 鉴权：验证当前登录用户的角色必须是child，否则抛出403异常
        var user = SecurityUtils.requireRole(UserRole.child);
        // 调用Service保存监护规则，并返回给前端
        return guardRuleService.saveGuardRule(elderId, request);
    }
}
