package com.smartelderly.service.location;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.location.dto.ActivityAlertResponse;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.location.ActivityLog;
import com.smartelderly.domain.location.ActivityLogRepository;

@Service
public class ActivityAlertService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final int MAX_PRESENCE_MESSAGES = 10;

    private final ActivityLogRepository activityLogRepository;
    private final ElderProfileRepository elderProfileRepository;

    public ActivityAlertService(
            ActivityLogRepository activityLogRepository,
            ElderProfileRepository elderProfileRepository) {
        this.activityLogRepository = activityLogRepository;
        this.elderProfileRepository = elderProfileRepository;
    }

    /**
     * 获取老人出门/回家位置消息（最多 10 条，按时间倒序）。
     */
    public ApiResponse<List<ActivityAlertResponse>> getActivityAlerts(Long elderId) {
        elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        List<String> types = List.of(ActivityLog.TYPE_GO_OUT, ActivityLog.TYPE_COME_HOME);
        List<ActivityLog> logs = activityLogRepository.findByElderProfileIdAndActivityTypeInOrderByStartTimeDesc(
                elderId,
                types,
                PageRequest.of(0, MAX_PRESENCE_MESSAGES));

        List<ActivityAlertResponse> responseList = logs.stream()
                .map(this::toPresenceMessage)
                .collect(Collectors.toList());

        return ApiResponse.ok(responseList);
    }

    private ActivityAlertResponse toPresenceMessage(ActivityLog log) {
        boolean goOut = ActivityLog.TYPE_GO_OUT.equals(log.getActivityType());
        String title = goOut ? "老人已出门" : "老人已回家";
        String content = goOut
                ? "检测到老人离开家的范围，请关注出行安全。"
                : "检测到老人回到家的范围内。";
        return new ActivityAlertResponse(
                log.getId(),
                log.getActivityType(),
                "geofence",
                title,
                content,
                log.getStartTime() != null ? log.getStartTime().format(ISO_FORMATTER) : null);
    }
}
