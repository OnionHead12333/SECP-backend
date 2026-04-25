package com.smartelderly.service.location;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.smartelderly.api.location.dto.LocationTrackDTO;
import com.smartelderly.domain.location.LocationLog;
import com.smartelderly.domain.location.LocationLogRepository;
import com.smartelderly.domain.location.LocationSource;
import com.smartelderly.domain.location.LocationType;

@Service
public class LocationTrackService {

    private final LocationLogRepository locationLogRepository;
    private final ElderLocationGuardService elderLocationGuardService;
    
    public LocationTrackService(LocationLogRepository locationLogRepository,
                                ElderLocationGuardService elderLocationGuardService) {
        this.locationLogRepository = locationLogRepository;
        this.elderLocationGuardService = elderLocationGuardService;
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
            LocalDateTime recordedAt = LocalDateTime.parse(dto.getRecordedAt(), DateTimeFormatter.ISO_DATE_TIME);
            log.setRecordedAt(recordedAt);
        } else {
            log.setRecordedAt(LocalDateTime.now());
        }
        
        LocationLog saved = locationLogRepository.save(log);
        elderLocationGuardService.markUploadSuccess(elderProfileId, saved.getRecordedAt());
        return saved;
    }
}
