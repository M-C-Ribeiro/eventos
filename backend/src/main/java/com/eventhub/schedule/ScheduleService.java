package com.eventhub.schedule;

import com.eventhub.access.Permission;
import com.eventhub.common.ApiException;
import com.eventhub.event.Event;
import com.eventhub.event.EventAccess;
import com.eventhub.event.EventService;
import com.eventhub.schedule.ScheduleDtos.ScheduleItemRequest;
import com.eventhub.schedule.ScheduleDtos.ScheduleItemResponse;
import com.eventhub.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Cronograma do evento. Quem pode ver o evento vê o cronograma;
 * para alterar é preciso ter permissão de editar o evento (perfil, responsável ou função na equipe).
 */
@Service
public class ScheduleService {

    private static final String NOT_FOUND = "Item do cronograma não encontrado.";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ScheduleItemRepository repository;
    private final EventService eventService;
    private final EventAccess eventAccess;

    public ScheduleService(ScheduleItemRepository repository, EventService eventService, EventAccess eventAccess) {
        this.repository = repository;
        this.eventService = eventService;
        this.eventAccess = eventAccess;
    }

    @Transactional(readOnly = true)
    public List<ScheduleItemResponse> list(Long eventId) {
        eventService.getVisibleEntity(eventId);
        return repository.findByEventIdOrderByDateAscTimeAscIdAsc(eventId).stream()
                .map(ScheduleItemResponse::from).toList();
    }

    @Transactional
    public ScheduleItemResponse create(Long eventId, ScheduleItemRequest req) {
        Event event = editableEvent(eventId);
        ScheduleItem item = new ScheduleItem();
        item.setEvent(event);
        apply(item, event, req);
        return ScheduleItemResponse.from(repository.save(item));
    }

    @Transactional
    public ScheduleItemResponse update(Long eventId, Long itemId, ScheduleItemRequest req) {
        Event event = editableEvent(eventId);
        ScheduleItem item = getItem(eventId, itemId);
        apply(item, event, req);
        return ScheduleItemResponse.from(item);
    }

    @Transactional
    public void delete(Long eventId, Long itemId) {
        editableEvent(eventId);
        repository.delete(getItem(eventId, itemId));
    }

    private Event editableEvent(Long eventId) {
        Event event = eventService.getEntity(eventId);
        eventAccess.require(event, Permission.EVENT_EDIT);
        return event;
    }

    private ScheduleItem getItem(Long eventId, Long itemId) {
        ScheduleItem item = repository.findById(itemId).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        CurrentUser.assertSameTenant(item.getTenantId(), NOT_FOUND);
        if (!item.getEvent().getId().equals(eventId)) {
            throw ApiException.notFound(NOT_FOUND);
        }
        return item;
    }

    private static void apply(ScheduleItem item, Event event, ScheduleItemRequest req) {
        LocalDate first = event.getStartAt().toLocalDate();
        LocalDate last = event.getEndAt().toLocalDate();
        if (req.date().isBefore(first) || req.date().isAfter(last)) {
            throw ApiException.badRequest(first.equals(last)
                    ? "A data deve ser a do evento: " + first.format(BR_DATE) + "."
                    : "A data deve estar entre " + first.format(BR_DATE) + " e " + last.format(BR_DATE) + ".");
        }
        item.setTitle(req.title().trim());
        item.setDate(req.date());
        item.setTime(req.time());
        item.setDurationMinutes(req.durationMinutes());
        item.setSpeaker(req.speaker().trim());
        item.setContact(req.contact() == null || req.contact().isBlank() ? null : req.contact().trim());
    }
}
