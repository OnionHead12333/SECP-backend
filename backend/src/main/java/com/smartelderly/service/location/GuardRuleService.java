package com.smartelderly.service.location;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.GuardRuleResponse;
import com.smartelderly.api.location.dto.SaveGuardRuleRequest;
import com.smartelderly.api.location.dto.SaveGuardRuleResponse;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.location.ElderGuardRule;
import com.smartelderly.domain.location.ElderGuardRuleRepository;

@Service
public class GuardRuleService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final ElderGuardRuleRepository guardRuleRepository;
    private final ElderProfileRepository elderProfileRepository;

    public GuardRuleService(ElderGuardRuleRepository guardRuleRepository,
            ElderProfileRepository elderProfileRepository) {
        this.guardRuleRepository = guardRuleRepository;
        this.elderProfileRepository = elderProfileRepository;
    }

    /**
     * 获取老人的监护规则
     * @param elderId 老人ID
     * @return ApiResponse
     */
    public ApiResponse<GuardRuleResponse> getGuardRule(Long elderId) {
        // 验证老人是否存在
        var elder = elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 查询监护规则
        ElderGuardRule rule = guardRuleRepository.findByElderProfileId(elderId)
                .orElseThrow(() -> new ApiException(4041, "guard rule not found"));

        // 转换为响应对象
        GuardRuleResponse response = new GuardRuleResponse(
                rule.getEnabled(),
                rule.getActiveStartTime().format(TIME_FORMATTER),
                rule.getActiveEndTime().format(TIME_FORMATTER),
                rule.getHomeInactivityMinutes(),
                rule.getOutsideInactivityMinutes(),
                rule.getAlertMinIntervalMinutes()
        );

        return ApiResponse.ok(response);
    }

    /**
     * 保存老人的监护规则
     * 如果规则不存在则创建，如果已存在则更新
     * @param elderId 老人ID
     * @param request 保存请求
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<SaveGuardRuleResponse> saveGuardRule(Long elderId, SaveGuardRuleRequest request) {
        // 验证老人是否存在
        var elder = elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 解析时间字符串为 LocalTime
        LocalTime activeStartTime = LocalTime.parse(request.getActiveStartTime(), TIME_FORMATTER);
        LocalTime activeEndTime = LocalTime.parse(request.getActiveEndTime(), TIME_FORMATTER);

        // 查询是否已存在规则
        var existingRule = guardRuleRepository.findByElderProfileId(elderId);
        
        ElderGuardRule rule;
        if (existingRule.isPresent()) {
            // 更新现有规则
            rule = existingRule.get();
            rule.setEnabled(request.getEnabled());
            rule.setActiveStartTime(activeStartTime);
            rule.setActiveEndTime(activeEndTime);
            rule.setHomeInactivityMinutes(request.getHomeInactivityMinutes());
            rule.setOutsideInactivityMinutes(request.getOutsideInactivityMinutes());
            rule.setAlertMinIntervalMinutes(request.getAlertMinIntervalMinutes());
            rule.setUpdatedAt(LocalDateTime.now());
        } else {
            // 创建新规则
            rule = new ElderGuardRule();
            rule.setElderProfileId(elderId);
            rule.setEnabled(request.getEnabled());
            rule.setActiveStartTime(activeStartTime);
            rule.setActiveEndTime(activeEndTime);
            rule.setHomeInactivityMinutes(request.getHomeInactivityMinutes());
            rule.setOutsideInactivityMinutes(request.getOutsideInactivityMinutes());
            rule.setAlertMinIntervalMinutes(request.getAlertMinIntervalMinutes());
            rule.setCreatedAt(LocalDateTime.now());
            rule.setUpdatedAt(LocalDateTime.now());
        }

        // 保存或更新规则
        guardRuleRepository.save(rule);

        // 返回成功响应
        SaveGuardRuleResponse response = new SaveGuardRuleResponse(elderId);
        return ApiResponse.ok("saved", response);
    }
}
