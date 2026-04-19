package com.smartelderly.service.location;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.domain.AlertStatus;
import com.smartelderly.domain.AlertType;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.EmergencyAlert;
import com.smartelderly.domain.EmergencyAlertRepository;
import com.smartelderly.domain.TriggerMode;
import com.smartelderly.domain.location.ElderGuardRule;
import com.smartelderly.domain.location.ElderGuardRuleRepository;
import com.smartelderly.domain.location.Geofence;
import com.smartelderly.domain.location.GeofenceRepository;
import com.smartelderly.domain.location.LocationLog;
import com.smartelderly.domain.location.LocationLogRepository;

/**
 * 活动状态分析服务
 * 负责判断老人的在家/外出状态，以及是否长时间未活动
 */
@Service
public class ActivityAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ActivityAnalysisService.class);

    // 位移距离阈值（单位：米）：在指定时间窗口内，位移小于此值视为未活动
    private static final double INACTIVITY_DISPLACEMENT_THRESHOLD = 30.0;

    private final LocationLogRepository locationLogRepository;
    private final GeofenceRepository geofenceRepository;
    private final ElderGuardRuleRepository guardRuleRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final EmergencyAlertRepository emergencyAlertRepository;
    private final GeofenceAnalysisHelper geofenceAnalysisHelper;

    public ActivityAnalysisService(
            LocationLogRepository locationLogRepository,
            GeofenceRepository geofenceRepository,
            ElderGuardRuleRepository guardRuleRepository,
            ElderProfileRepository elderProfileRepository,
            EmergencyAlertRepository emergencyAlertRepository,
            GeofenceAnalysisHelper geofenceAnalysisHelper) {
        this.locationLogRepository = locationLogRepository;
        this.geofenceRepository = geofenceRepository;
        this.guardRuleRepository = guardRuleRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.emergencyAlertRepository = emergencyAlertRepository;
        this.geofenceAnalysisHelper = geofenceAnalysisHelper;
    }

    /**
     * 定时执行的分析任务
     * 遍历所有启用的监护规则，判断是否需要生成未活动提醒
     * @return 生成的提醒数量
     */
    @Transactional
    public int runAnalysisJob() {
        int alertCount = 0;

        try {
            // 1. 获取所有启用的监护规则
            List<ElderGuardRule> enabledRules = guardRuleRepository.findByEnabled(true);

            for (ElderGuardRule rule : enabledRules) {
                // 2. 获取老人信息和最新位置
                Optional<ElderProfile> elderOpt = elderProfileRepository.findById(rule.getElderProfileId());
                if (elderOpt.isEmpty()) {
                    log.warn("Elder profile not found for rule: {}", rule.getId());
                    continue;
                }

                Optional<LocationLog> latestLocationOpt = locationLogRepository
                        .findFirstByElderProfileIdOrderByRecordedAtDesc(rule.getElderProfileId());
                if (latestLocationOpt.isEmpty()) {
                    log.debug("No location found for elder: {}", rule.getElderProfileId());
                    continue;
                }

                LocationLog latestLocation = latestLocationOpt.get();

                // 3. 判断当前时刻是否在活跃时段
                LocalTime now = LocalTime.now();
                if (!isInActiveTime(now, rule.getActiveStartTime(), rule.getActiveEndTime())) {
                    // 不在活跃时段，跳过
                    continue;
                }

                // 4. 判断老人是否在家
                boolean isHome = isLocationHome(rule.getElderProfileId(), latestLocation);

                // 5. 获取应用的不活动阈值（分钟）
                int inactivityMinutes = isHome ? rule.getHomeInactivityMinutes()
                        : rule.getOutsideInactivityMinutes();

                // 6. 判断最后位置更新距离现在是否超过阈值
                LocalDateTime lastUpdateTime = latestLocation.getRecordedAt();
                if (lastUpdateTime == null) {
                    lastUpdateTime = LocalDateTime.now();
                }
                long minutesSinceLastUpdate = java.time.temporal.ChronoUnit.MINUTES
                        .between(lastUpdateTime, LocalDateTime.now());

                if (minutesSinceLastUpdate >= inactivityMinutes) {
                    // 7. 检查是否已在最小提醒间隔内生成过相同提醒
                    if (!hasRecentInactivityAlert(rule.getElderProfileId(), rule.getAlertMinIntervalMinutes())) {
                        // 8. 生成未活动提醒
                        if (createInactivityAlert(rule.getElderProfileId(), isHome, inactivityMinutes)) {
                            alertCount++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in activity analysis job", e);
        }

        if (alertCount > 0) {
            log.info("Activity analysis job generated {} inactivity alert(s)", alertCount);
        }

        return alertCount;
    }

    /**
     * 判断当前时刻是否在活跃时段内
     */
    private boolean isInActiveTime(LocalTime now, LocalTime startTime, LocalTime endTime) {
        // 简单情况：活跃时段不跨天
        if (startTime.isBefore(endTime)) {
            return !now.isBefore(startTime) && now.isBefore(endTime);
        }
        // 复杂情况：活跃时段跨天（暂不支持，当前版本只支持单一活跃时段）
        return !now.isBefore(startTime) || now.isBefore(endTime);
    }

    /**
     * 判断位置是否在家（基于所有启用的围栏）
     * 只要位置在任何一个启用的围栏范围内，就判断为在家
     */
    private boolean isLocationHome(Long elderProfileId, LocationLog location) {
        // 获取该老人所有启用的围栏
        List<Geofence> enabledGeofences = geofenceRepository.findByElderProfileIdAndIsEnabled(
                elderProfileId, true);

        // 使用工具类判断位置是否在围栏范围内
        return geofenceAnalysisHelper.isLocationWithinGeofences(location, enabledGeofences);
    }

    /**
     * 检查是否在最小间隔内已生成过同类提醒
     */
    private boolean hasRecentInactivityAlert(Long elderProfileId, int minIntervalMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minIntervalMinutes);

        // 查询在cutoffTime之后生成的inactivity类型告警
        List<EmergencyAlert> recentAlerts = emergencyAlertRepository
                .findByElderProfileIdAndAlertTypeAndTriggerModeAndTriggerTimeAfter(
                        elderProfileId,
                        AlertType.inactivity,
                        TriggerMode.rule_engine,
                        cutoffTime);

        return !recentAlerts.isEmpty();
    }

    /**
     * 生成未活动提醒
     */
    private boolean createInactivityAlert(Long elderProfileId, boolean isHome, int inactivityMinutes) {
        try {
            EmergencyAlert alert = new EmergencyAlert();
            alert.setElderProfileId(elderProfileId);
            alert.setAlertType(AlertType.inactivity);
            alert.setTriggerMode(TriggerMode.rule_engine);
            alert.setStatus(AlertStatus.pending_revoke);
            alert.setTriggerTime(LocalDateTime.now());
            alert.setCreatedAt(LocalDateTime.now());

            // 设置撤销截止时间（例如：5分钟后）
            alert.setRevokeDeadline(LocalDateTime.now().plusMinutes(5));

            // 设置备注说明
            String locationDesc = isHome ? "在家" : "外出";
            String remark = String.format("%s%d分钟未检测到明显移动", locationDesc, inactivityMinutes);
            alert.setRemark(remark);

            emergencyAlertRepository.save(alert);
            log.info("Created inactivity alert for elder: {}, isHome: {}, inactivityMinutes: {}",
                    elderProfileId, isHome, inactivityMinutes);
            return true;
        } catch (Exception e) {
            log.error("Failed to create inactivity alert for elder: {}", elderProfileId, e);
            return false;
        }
    }
}
