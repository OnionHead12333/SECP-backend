package com.smartelderly.api.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 锻炼提醒响应体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseReminderResponse {
    private Long id;
    private Long elderProfileId;
    private String title;
    private String exerciseType;
    private String startTime;      // HH:mm:ss
    private String endTime;        // HH:mm:ss
    private String repeatRule;
    private String status;
    
    // 可选字段，前端不展示但可接收
    private Integer goalValue;
    private String goalUnit;
    private Integer intervalMinutes;
}
