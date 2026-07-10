package com.smartelderly.service;

import com.smartelderly.api.dto.MedicineReminderConfirmDTO;
import com.smartelderly.api.dto.MedicineReminderProgressDTO;
import com.smartelderly.api.dto.MedicineReminderSnoozeDTO;

public interface ElderMedicineReminderService {

    MedicineReminderProgressDTO getTodayProgress(Long elderId);

    MedicineReminderProgressDTO confirmMedicine(Long reminderId, MedicineReminderConfirmDTO confirmDTO);

    MedicineReminderProgressDTO snoozeReminder(Long reminderId, MedicineReminderSnoozeDTO snoozeDTO);
}
