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
    
    // 构造函数，使用构造函数注入LocationLogRepository（准备好Repository）
    public LocationTrackService(LocationLogRepository locationLogRepository) {
        this.locationLogRepository = locationLogRepository;
    }
    
    //保存位置轨迹信息的方法，供Controller调用
    public LocationLog saveLocation(Long elderProfileId, LocationTrackDTO dto) {
        //第1步：实例化新的LocationLog对象
        LocationLog log = new LocationLog();

        //第2步：设置LocationLog对象的属性（elderProfileId由Controller传入）
        log.setElderProfileId(elderProfileId);
        log.setLatitude(dto.getLatitude());
        log.setLongitude(dto.getLongitude());
        
        // 第3步：安全地解析locationType枚举
        if (dto.getLocationType() != null) {
            try {
                log.setLocationType(LocationType.valueOf(dto.getLocationType()));
            } catch (IllegalArgumentException e) {
                 // 如果值无效，默认为outdoor
                log.setLocationType(LocationType.outdoor);
            }
        } else {
            log.setLocationType(LocationType.outdoor);
        }
        
        // 第4步：安全地解析source枚举
        if (dto.getSource() != null) {
            try {
                log.setSource(LocationSource.valueOf(dto.getSource()));
            } catch (IllegalArgumentException e) {
                // 如果值无效，默认为gaode
                log.setSource(LocationSource.gaode);
            }
        } else {
            log.setSource(LocationSource.gaode);
        }
        
        // 第5步：解析ISO 8601 datetime
        if (dto.getRecordedAt() != null) {
            LocalDateTime recordedAt = LocalDateTime.parse(dto.getRecordedAt(), DateTimeFormatter.ISO_DATE_TIME);
            log.setRecordedAt(recordedAt);
        } else {
            // 如果没传时间，取系统当前时间
            log.setRecordedAt(LocalDateTime.now());
        }
        
        //第6步：保存LocationLog对象到数据库
        return locationLogRepository.save(log);
    }
}
