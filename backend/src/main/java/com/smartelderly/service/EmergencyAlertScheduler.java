package com.smartelderly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smartelderly.service.location.ActivityAnalysisService;

@Component
public class EmergencyAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmergencyAlertScheduler.class);

    private final EmergencyAlertService emergencyAlertService;
    private final ActivityAnalysisService activityAnalysisService;

    public EmergencyAlertScheduler(EmergencyAlertService emergencyAlertService,
            ActivityAnalysisService activityAnalysisService) {
        this.emergencyAlertService = emergencyAlertService;
        this.activityAnalysisService = activityAnalysisService;
    }

    // 每隔1秒执行一次，自动发送过期未撤销的告警
    @Scheduled(fixedDelay = 1000)
    public void autoSendPastDeadline() {
        int n = emergencyAlertService.runAutoSendJob();
        if (n > 0) {
            log.debug("Auto-sent {} emergency alert(s) past revoke deadline", n);
        }
    }

    // 每隔1分钟执行一次，分析活动状态并生成未活动提醒
    @Scheduled(fixedDelay = 60000)
    public void analyzeActivityStatus() {
        int n = activityAnalysisService.runAnalysisJob();
        if (n > 0) {
            log.debug("Activity analysis job generated {} inactivity alert(s)", n);
        }
    }
}
