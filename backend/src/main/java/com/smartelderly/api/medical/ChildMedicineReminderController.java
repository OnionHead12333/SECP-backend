package com.smartelderly.api.medical;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.medical.dto.ChildMedicineReminderCreateRequest;
import com.smartelderly.api.medical.dto.ChildMedicineReminderUpdateRequest;
import com.smartelderly.api.medical.dto.MedicineReminderViewDto;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.AuthPrincipal;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.medical.ChildMedicineReminderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/child/medicine-reminders")
@Validated
public class ChildMedicineReminderController {

    private final ChildMedicineReminderService childMedicineReminderService;

    public ChildMedicineReminderController(ChildMedicineReminderService childMedicineReminderService) {
        this.childMedicineReminderService = childMedicineReminderService;
    }

    @GetMapping
    public ApiResponse<List<MedicineReminderViewDto>> listMedicineReminders(
            @RequestParam("elderProfileId") Long elderProfileId) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        return ApiResponse.ok(childMedicineReminderService.listMedicineReminders(principal, elderProfileId));
    }

    @PostMapping
    public ApiResponse<MedicineReminderViewDto> createMedicineReminder(
            @Valid @RequestBody ChildMedicineReminderCreateRequest request) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        return ApiResponse.ok("created", childMedicineReminderService.createMedicineReminder(principal, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicineReminderViewDto> updateMedicineReminder(
            @PathVariable("id") Long reminderId,
            @Valid @RequestBody ChildMedicineReminderUpdateRequest request) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        return ApiResponse.ok(childMedicineReminderService.updateMedicineReminder(principal, reminderId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMedicineReminder(@PathVariable("id") Long reminderId) {
        AuthPrincipal principal = SecurityUtils.requireRole(UserRole.child);
        childMedicineReminderService.deleteMedicineReminder(principal, reminderId);
        return ApiResponse.ok(null);
    }
}
