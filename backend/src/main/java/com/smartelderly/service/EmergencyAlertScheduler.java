package com.smartelderly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmergencyAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmergencyAlertScheduler.class);

    private final EmergencyAlertService emergencyAlertService;

    public EmergencyAlertScheduler(EmergencyAlertService emergencyAlertService) {
        this.emergencyAlertService = emergencyAlertService;
    }

    @Scheduled(fixedDelay = 1000)
    public void autoSendPastDeadline() {
        int n = emergencyAlertService.runAutoSendJob();
        if (n > 0) {
            log.debug("Auto-sent {} emergency alert(s) past revoke deadline", n);
        }
    }
}
