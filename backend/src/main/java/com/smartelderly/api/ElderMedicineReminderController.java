package com.smartelderly.api;

import com.smartelderly.api.dto.MedicineReminderConfirmDTO;
import com.smartelderly.api.dto.MedicineReminderProgressDTO;
import com.smartelderly.api.dto.MedicineReminderSnoozeDTO;
import com.smartelderly.security.ElderOwnedProfileResolver;
import com.smartelderly.service.ElderMedicineReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/elder/medicine-reminders")
@Validated
public class ElderMedicineReminderController {

    @Autowired
    private ElderMedicineReminderService reminderService;

    @Autowired
    private ElderOwnedProfileResolver elderOwnedProfileResolver;

    @GetMapping("/today-progress")
    public ApiResponse<MedicineReminderProgressDTO> getTodayProgress(@RequestParam("elderId") Long elderId) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(elderId);
        return ApiResponse.ok(reminderService.getTodayProgress(profileId));
    }

    @PostMapping("/{reminderId}/confirm")
    public ApiResponse<MedicineReminderProgressDTO> confirmMedicine(
            @PathVariable Long reminderId,
            @RequestBody MedicineReminderConfirmDTO confirmDTO) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(Long.valueOf(confirmDTO.elderId));
        confirmDTO.elderId = Math.toIntExact(profileId);
        return ApiResponse.ok(reminderService.confirmMedicine(reminderId, confirmDTO));
    }

    @PostMapping("/{reminderId}/snooze")
    public ApiResponse<MedicineReminderProgressDTO> snoozeReminder(
            @PathVariable Long reminderId,
            @RequestBody MedicineReminderSnoozeDTO snoozeDTO) {
        long profileId = elderOwnedProfileResolver.resolveRequestElderId(Long.valueOf(snoozeDTO.elderId));
        snoozeDTO.elderId = Math.toIntExact(profileId);
        return ApiResponse.ok(reminderService.snoozeReminder(reminderId, snoozeDTO));
    }
}
