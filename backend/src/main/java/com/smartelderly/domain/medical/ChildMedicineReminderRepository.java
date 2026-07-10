package com.smartelderly.domain.medical;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildMedicineReminderRepository extends JpaRepository<ChildMedicineReminder, Long> {

    List<ChildMedicineReminder> findByElderProfileIdOrderByRemindTimeAsc(Long elderProfileId);

    Optional<ChildMedicineReminder> findByIdAndElderProfileId(Long id, Long elderProfileId);
}
