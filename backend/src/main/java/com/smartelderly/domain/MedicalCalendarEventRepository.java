package com.smartelderly.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalCalendarEventRepository extends JpaRepository<MedicalCalendarEvent, Long> {

    List<MedicalCalendarEvent> findByElderProfileIdAndStartAtBetweenOrderByStartAtAsc(
            Long elderProfileId, LocalDateTime fromInclusive, LocalDateTime toInclusive);

    List<MedicalCalendarEvent> findByElderProfileIdAndEventTypeAndStartAtBetweenOrderByStartAtAsc(
            Long elderProfileId,
            MedicalCalendarEventType eventType,
            LocalDateTime fromInclusive,
            LocalDateTime toInclusive);

    List<MedicalCalendarEvent> findByElderProfileIdAndSourceDocumentId(
            Long elderProfileId, Long sourceDocumentId);

    Optional<MedicalCalendarEvent> findByIdAndElderProfileId(Long id, Long elderProfileId);
}
