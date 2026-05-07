package com.smartelderly.api;

import com.smartelderly.api.dto.MedicineReminderProgressDTO;
import com.smartelderly.api.dto.MedicineReminderConfirmDTO;
import com.smartelderly.api.dto.MedicineReminderSnoozeDTO;
import com.smartelderly.service.ElderMedicineReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/elder/medicine-reminders")
public class ElderMedicineReminderController {

    @Autowired
    private ElderMedicineReminderService reminderService;

    /**
     * 查询今日吃药进度
     */
    @GetMapping("/today-progress")
    public ApiResponse<MedicineReminderProgressDTO> getTodayProgress(@RequestParam Integer elderId) {
        MedicineReminderProgressDTO progress = reminderService.getTodayProgress(elderId);
        return ApiResponse.ok(progress);
    }

    /**
     * 确认已吃药
     */
    @PostMapping("/{reminderId}/confirm")
    public ApiResponse<MedicineReminderProgressDTO> confirmMedicine(@PathVariable Integer reminderId,
            @RequestBody MedicineReminderConfirmDTO confirmDTO) {
        MedicineReminderProgressDTO progress = reminderService.confirmMedicine(reminderId, confirmDTO);
        return ApiResponse.ok(progress);
    }

    /**
     * 稍后提醒一次
     */
    @PostMapping("/{reminderId}/snooze")
    public ApiResponse<MedicineReminderProgressDTO> snoozeReminder(@PathVariable Integer reminderId,
            @RequestBody MedicineReminderSnoozeDTO snoozeDTO) {
        MedicineReminderProgressDTO progress = reminderService.snoozeReminder(reminderId, snoozeDTO);
        return ApiResponse.ok(progress);
    }
}