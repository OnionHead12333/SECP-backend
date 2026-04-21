package com.smartelderly.service.location;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.ActivityAlertResponse;
import com.smartelderly.domain.AlertType;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.EmergencyAlert;
import com.smartelderly.domain.EmergencyAlertRepository;
import com.smartelderly.domain.TriggerMode;

@Service
public class ActivityAlertService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final EmergencyAlertRepository emergencyAlertRepository;
    private final ElderProfileRepository elderProfileRepository;

    public ActivityAlertService(EmergencyAlertRepository emergencyAlertRepository,
            ElderProfileRepository elderProfileRepository) {
        this.emergencyAlertRepository = emergencyAlertRepository;
        this.elderProfileRepository = elderProfileRepository;
    }

    /**
     * 获取未活动提醒列表
     * @param elderId 老人ID
     * @return ApiResponse 包含未活动提醒列表
     */
    public ApiResponse<List<ActivityAlertResponse>> getActivityAlerts(Long elderId) {
        // 验证老人是否存在
        var elder = elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 查询未活动提醒（alertType=inactivity, triggerMode=rule_engine）
        List<EmergencyAlert> alerts = emergencyAlertRepository
                .findByElderProfileIdAndAlertTypeAndTriggerModeOrderByTriggerTimeDesc(
                        elderId,
                        AlertType.inactivity,
                        TriggerMode.rule_engine
                );

        // 转换为响应对象列表
        List<ActivityAlertResponse> responseList = alerts.stream()
                .map(alert -> new ActivityAlertResponse(
                        alert.getId(),
                        alert.getAlertType().name(),
                        alert.getTriggerMode().name(),
                        "老人长时间未活动", // 固定标题(title)
                        "活跃时段内外出超过设定时间未检测到明显移动", // 固定内容(content)
                        alert.getTriggerTime() != null ? alert.getTriggerTime().format(ISO_FORMATTER) : null
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(responseList);
    }
}
