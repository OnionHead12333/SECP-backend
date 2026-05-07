package com.smartelderly.service;
import org.springframework.stereotype.Service;

import com.smartelderly.api.dto.MedicineReminderProgressDTO;
import com.smartelderly.api.dto.MedicineReminderConfirmDTO;
import com.smartelderly.api.dto.MedicineReminderSnoozeDTO;

public interface ElderMedicineReminderService {
    MedicineReminderProgressDTO getTodayProgress(Integer elderId);
    MedicineReminderProgressDTO confirmMedicine(Integer reminderId, MedicineReminderConfirmDTO confirmDTO);
    MedicineReminderProgressDTO snoozeReminder(Integer reminderId, MedicineReminderSnoozeDTO snoozeDTO);
}