package com.smartelderly.service.location;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.smartelderly.api.location.dto.LocationTrackDTO;
import com.smartelderly.domain.location.LocationLog;
import com.smartelderly.domain.location.LocationLogRepository;
import com.smartelderly.domain.location.LocationSource;
import com.smartelderly.domain.location.LocationType;
import com.smartelderly.util.TimeUtils;

@Service
public class LocationTrackService {

    private final LocationLogRepository locationLogRepository;
    private final ElderLocationGuardService elderLocationGuardService;
    private final ElderPresenceService elderPresenceService;
    
    public LocationTrackService(LocationLogRepository locationLogRepository,
                                ElderLocationGuardService elderLocationGuardService,
                                ElderPresenceService elderPresenceService) {
        this.locationLogRepository = locationLogRepository;
        this.elderLocationGuardService = elderLocationGuardService;
        this.elderPresenceService = elderPresenceService;
    }
    
    public LocationLog saveLocation(Long elderProfileId, LocationTrackDTO dto) {
        LocationLog log = new LocationLog();

        log.setElderProfileId(elderProfileId);
        log.setLatitude(dto.getLatitude());
        log.setLongitude(dto.getLongitude());
        
        if (dto.getLocationType() != null) {
            try {
                log.setLocationType(LocationType.valueOf(dto.getLocationType()));
            } catch (IllegalArgumentException e) {
                log.setLocationType(LocationType.outdoor);
            }
        } else {
            log.setLocationType(LocationType.outdoor);
        }
        
        if (dto.getSource() != null) {
            try {
                log.setSource(LocationSource.valueOf(dto.getSource()));
            } catch (IllegalArgumentException e) {
                log.setSource(LocationSource.gaode);
            }
        } else {
            log.setSource(LocationSource.gaode);
        }
        
        if (dto.getRecordedAt() != null) {
            log.setRecordedAt(parseRecordedAt(dto.getRecordedAt()));
        } else {
            log.setRecordedAt(TimeUtils.now());
        }
        
        LocationLog saved = locationLogRepository.save(log);
        elderLocationGuardService.markUploadSuccess(elderProfileId, saved.getRecordedAt());
        elderPresenceService.checkAfterLocationUpload(elderProfileId, saved);
        return saved;
    }

    /** 客户端上传 UTC Instant（带 Z）；落库为 Asia/Shanghai 墙钟，避免差 8 小时。 */
    private static LocalDateTime parseRecordedAt(String iso8601) {
        try {
            return LocalDateTime.ofInstant(Instant.parse(iso8601), TimeUtils.APP_ZONE);
        } catch (Exception ignored) {
            return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_DATE_TIME);
        }
    }
}
