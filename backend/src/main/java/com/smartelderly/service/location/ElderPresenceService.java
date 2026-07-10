package com.smartelderly.service.location;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.domain.location.ActivityLog;
import com.smartelderly.domain.location.ActivityLogRepository;
import com.smartelderly.domain.location.Geofence;
import com.smartelderly.domain.location.GeofenceRepository;
import com.smartelderly.domain.location.LocationLog;

@Service
public class ElderPresenceService {

    private static final int MAX_PRESENCE_MESSAGES = 10;
    private static final Duration MIN_EVENT_GAP = Duration.ofMinutes(2);
    private static final List<String> PRESENCE_TYPES = List.of(
            ActivityLog.TYPE_GO_OUT, ActivityLog.TYPE_COME_HOME);

    private final GeofenceRepository geofenceRepository;
    private final GeofenceAnalysisHelper geofenceAnalysisHelper;
    private final ActivityLogRepository activityLogRepository;

    public ElderPresenceService(
            GeofenceRepository geofenceRepository,
            GeofenceAnalysisHelper geofenceAnalysisHelper,
            ActivityLogRepository activityLogRepository) {
        this.geofenceRepository = geofenceRepository;
        this.geofenceAnalysisHelper = geofenceAnalysisHelper;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void checkAfterLocationUpload(Long elderProfileId, LocationLog saved) {
        List<Geofence> geofences = geofenceRepository.findByElderProfileIdAndIsEnabled(elderProfileId, true);
        if (geofences.isEmpty()) {
            return;
        }

        boolean isHomeNow = geofenceAnalysisHelper.isLocationWithinGeofences(saved, geofences);
        LocalDateTime eventTime = saved.getRecordedAt() != null ? saved.getRecordedAt() : LocalDateTime.now();

        ActivityLog last = activityLogRepository
                .findFirstByElderProfileIdAndActivityTypeInOrderByStartTimeDesc(elderProfileId, PRESENCE_TYPES)
                .orElse(null);

        if (last != null) {
            Duration gap = Duration.between(last.getStartTime(), eventTime);
            if (gap.compareTo(MIN_EVENT_GAP) < 0) {
                return;
            }
        }

        String lastType = last == null ? null : last.getActivityType();
        if (lastType == null) {
            if (!isHomeNow) {
                recordPresenceEvent(elderProfileId, ActivityLog.TYPE_GO_OUT, eventTime);
            }
            return;
        }

        if (ActivityLog.TYPE_COME_HOME.equals(lastType) && !isHomeNow) {
            recordPresenceEvent(elderProfileId, ActivityLog.TYPE_GO_OUT, eventTime);
        } else if (ActivityLog.TYPE_GO_OUT.equals(lastType) && isHomeNow) {
            recordPresenceEvent(elderProfileId, ActivityLog.TYPE_COME_HOME, eventTime);
        }
    }

    private void recordPresenceEvent(Long elderProfileId, String activityType, LocalDateTime eventTime) {
        ActivityLog log = new ActivityLog();
        log.setElderProfileId(elderProfileId);
        log.setActivityType(activityType);
        log.setStartTime(eventTime);
        activityLogRepository.save(log);
        pruneOldPresenceLogs(elderProfileId);
    }

    private void pruneOldPresenceLogs(Long elderProfileId) {
        List<ActivityLog> logs = activityLogRepository.findByElderProfileIdAndActivityTypeInOrderByStartTimeDesc(
                elderProfileId,
                PRESENCE_TYPES,
                PageRequest.of(0, MAX_PRESENCE_MESSAGES + 50));
        if (logs.size() <= MAX_PRESENCE_MESSAGES) {
            return;
        }
        for (int i = MAX_PRESENCE_MESSAGES; i < logs.size(); i++) {
            activityLogRepository.delete(logs.get(i));
        }
    }
}
