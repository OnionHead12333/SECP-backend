package com.smartelderly.api;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.medical.CreateMedicalCalendarEventRequest;
import com.smartelderly.api.dto.medical.MedicalCalendarEventViewDto;
import com.smartelderly.api.dto.medical.UpdateMedicalCalendarEventRequest;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.medical.MedicalCalendarService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/medical/calendar/events")
public class MedicalCalendarRestController {

    private final MedicalCalendarService medicalCalendarService;

    public MedicalCalendarRestController(MedicalCalendarService medicalCalendarService) {
        this.medicalCalendarService = medicalCalendarService;
    }

    @GetMapping
    public ApiResponse<java.util.List<MedicalCalendarEventViewDto>> list(
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "eventType", required = false) String eventType) {
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(
                medicalCalendarService.listEvents(principal, elderProfileId, from, to, eventType));
    }

    @PostMapping
    public ApiResponse<MedicalCalendarEventViewDto> create(
            @Valid @RequestBody CreateMedicalCalendarEventRequest request) {
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(medicalCalendarService.createEvent(principal, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicalCalendarEventViewDto> update(
            @PathVariable("id") long id,
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId,
            @Valid @RequestBody UpdateMedicalCalendarEventRequest request) {
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(
                medicalCalendarService.updateEvent(principal, elderProfileId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable("id") long id,
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId) {
        var principal = SecurityUtils.requireAuth();
        medicalCalendarService.deleteEvent(principal, elderProfileId, id);
        return ApiResponse.success(null);
    }
}
