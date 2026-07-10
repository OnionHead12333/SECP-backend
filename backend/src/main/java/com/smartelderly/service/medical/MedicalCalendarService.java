package com.smartelderly.service.medical;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.medical.CreateMedicalCalendarEventRequest;
import com.smartelderly.api.dto.medical.MedicalCalendarEventViewDto;
import com.smartelderly.api.dto.medical.UpdateMedicalCalendarEventRequest;
import com.smartelderly.domain.MedicalCalendarEvent;
import com.smartelderly.domain.MedicalCalendarEventRepository;
import com.smartelderly.domain.MedicalCalendarEventType;
import com.smartelderly.domain.MedicalDocumentRepository;
import com.smartelderly.security.AuthPrincipal;

@Service
public class MedicalCalendarService {

    private final MedicalAccessService medicalAccessService;
    private final MedicalCalendarEventRepository eventRepository;
    private final MedicalDocumentRepository documentRepository;

    public MedicalCalendarService(
            MedicalAccessService medicalAccessService,
            MedicalCalendarEventRepository eventRepository,
            MedicalDocumentRepository documentRepository) {
        this.medicalAccessService = medicalAccessService;
        this.eventRepository = eventRepository;
        this.documentRepository = documentRepository;
    }

    public List<MedicalCalendarEventViewDto> listEvents(
            AuthPrincipal principal,
            Long elderProfileId,
            LocalDateTime fromInclusive,
            LocalDateTime toInclusive,
            String eventType) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        MedicalCalendarEventType type = null;
        if (eventType != null && !eventType.isBlank()) {
            try {
                type = MedicalCalendarEventType.valueOf(eventType.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(4001, "非法 eventType，应为 EXAM / FOLLOWUP / MEDICATION");
            }
        }
        List<MedicalCalendarEvent> rows =
                type == null
                        ? eventRepository.findByElderProfileIdAndStartAtBetweenOrderByStartAtAsc(
                                eid, fromInclusive, toInclusive)
                        : eventRepository.findByElderProfileIdAndEventTypeAndStartAtBetweenOrderByStartAtAsc(
                                eid, type, fromInclusive, toInclusive);
        return rows.stream().map(MedicalCalendarService::toView).toList();
    }

    public MedicalCalendarEventViewDto createEvent(
            AuthPrincipal principal, CreateMedicalCalendarEventRequest req) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, req.getElderProfileId());
        MedicalCalendarEventType type = parseEventType(req.getEventType());
        if (req.getSourceDocumentId() != null) {
            documentRepository
                    .findByIdAndElderProfileId(req.getSourceDocumentId(), eid)
                    .orElseThrow(() -> new ApiException(4041, "来源单据不存在"));
        }
        MedicalCalendarEvent ev = new MedicalCalendarEvent();
        ev.setElderProfileId(eid);
        ev.setEventType(type);
        ev.setTitle(req.getTitle().trim());
        ev.setStartAt(req.getStartAt());
        ev.setEndAt(req.getEndAt());
        ev.setNotes(req.getNotes());
        ev.setSourceDocumentId(req.getSourceDocumentId());
        ev.setCreatedByUserId(principal.userId());
        ev = eventRepository.save(ev);
        return toView(ev);
    }

    public MedicalCalendarEventViewDto updateEvent(
            AuthPrincipal principal,
            Long elderProfileId,
            long eventId,
            UpdateMedicalCalendarEventRequest req) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        MedicalCalendarEvent ev =
                eventRepository
                        .findByIdAndElderProfileId(eventId, eid)
                        .orElseThrow(() -> new ApiException(4043, "日历事件不存在"));
        if (req.getEventType() != null && !req.getEventType().isBlank()) {
            ev.setEventType(parseEventType(req.getEventType()));
        }
        if (req.getTitle() != null) {
            ev.setTitle(req.getTitle().trim());
        }
        if (req.getStartAt() != null) {
            ev.setStartAt(req.getStartAt());
        }
        if (req.getEndAt() != null) {
            ev.setEndAt(req.getEndAt());
        }
        if (req.getNotes() != null) {
            ev.setNotes(req.getNotes());
        }
        ev = eventRepository.save(ev);
        return toView(ev);
    }

    public void deleteEvent(AuthPrincipal principal, Long elderProfileId, long eventId) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        MedicalCalendarEvent ev =
                eventRepository
                        .findByIdAndElderProfileId(eventId, eid)
                        .orElseThrow(() -> new ApiException(4043, "日历事件不存在"));
        eventRepository.delete(ev);
    }

    private static MedicalCalendarEventType parseEventType(String raw) {
        try {
            return MedicalCalendarEventType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(4001, "非法 eventType，应为 EXAM / FOLLOWUP / MEDICATION");
        }
    }

    private static MedicalCalendarEventViewDto toView(MedicalCalendarEvent e) {
        return MedicalCalendarEventViewDto.builder()
                .id(e.getId())
                .elderProfileId(e.getElderProfileId())
                .eventType(e.getEventType().name())
                .title(e.getTitle())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .notes(e.getNotes())
                .sourceDocumentId(e.getSourceDocumentId())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
