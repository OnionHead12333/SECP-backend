package com.smartelderly.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicineReminderRepository extends JpaRepository<MedicineReminder, Integer> {
    List<MedicineReminder> findByElderIdAndEnabled(int elderId, boolean enabled);
    List<MedicineReminder> findByElderIdAndRemindTimeBetweenAndEnabled(int elderId, java.time.LocalDateTime start, java.time.LocalDateTime end, boolean enabled);
}
