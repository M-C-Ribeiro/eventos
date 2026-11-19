package com.eventhub.registration;

import com.eventhub.access.Permission;
import com.eventhub.common.ApiException;
import com.eventhub.event.EventAccess;
import com.eventhub.event.Event;
import com.eventhub.event.EventRepository;
import com.eventhub.event.EventService;
import com.eventhub.event.EventStatus;
import com.eventhub.registration.RegistrationDtos.RegistrationRequest;
import com.eventhub.registration.RegistrationDtos.RegistrationResponse;
import com.eventhub.security.CurrentUser;
import com.eventhub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class RegistrationService {

    private static final String NOT_FOUND = "Inscrição não encontrada.";

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final UserRepository userRepository;
    private final EventAccess eventAccess;

    public RegistrationService(RegistrationRepository registrationRepository, EventRepository eventRepository,
                               EventService eventService, UserRepository userRepository, EventAccess eventAccess) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.eventAccess = eventAccess;
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> list(Long eventId) {
        eventAccess.require(eventService.getEntity(eventId), Permission.REGISTRATION_VIEW);
        return registrationRepository.findByEventIdOrderByNameAsc(eventId).stream()
                .map(RegistrationResponse::from).toList();
    }

    /** Inscrição feita por um organizador, para qualquer pessoa. */
    @Transactional
    public RegistrationResponse create(Long eventId, RegistrationRequest req) {
        Event event = lockEvent(eventId);
        eventAccess.require(event, Permission.REGISTRATION_MANAGE);
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw ApiException.badRequest("Não é possível inscrever em um evento cancelado ou concluído.");
        }
        return RegistrationResponse.from(register(event, req.name().trim(), req.email(), null));
    }

    @Transactional
    public RegistrationResponse checkIn(Long eventId, Long registrationId) {
        Registration reg = getEntity(eventId, registrationId, Permission.CHECKIN);
        if (reg.getStatus() == RegistrationStatus.CANCELLED) {
            throw ApiException.badRequest("Inscrição cancelada não pode fazer check-in.");
        }
        reg.setStatus(RegistrationStatus.CHECKED_IN);
        reg.setCheckedInAt(Instant.now());
        return RegistrationResponse.from(reg);
    }

    @Transactional
    public RegistrationResponse undoCheckIn(Long eventId, Long registrationId) {
        Registration reg = getEntity(eventId, registrationId, Permission.CHECKIN);
        if (reg.getStatus() == RegistrationStatus.CHECKED_IN) {
            reg.setStatus(RegistrationStatus.CONFIRMED);
            reg.setCheckedInAt(null);
        }
        return RegistrationResponse.from(reg);
    }

    @Transactional
    public RegistrationResponse cancel(Long eventId, Long registrationId) {
        Registration reg = getEntity(eventId, registrationId, Permission.REGISTRATION_MANAGE);
        reg.setStatus(RegistrationStatus.CANCELLED);
        reg.setCheckedInAt(null);
        return RegistrationResponse.from(reg);
    }

    private Registration register(Event event, String name, String rawEmail, Long userId) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        Registration existing = registrationRepository.findByEventIdAndEmailIgnoreCase(event.getId(), email).orElse(null);
        if (existing != null && existing.getStatus() != RegistrationStatus.CANCELLED) {
            throw ApiException.conflict("Este e-mail já está inscrito no evento.");
        }

        long confirmed = registrationRepository.countByEventIdAndStatusNot(event.getId(), RegistrationStatus.CANCELLED);
        if (event.getCapacity() != null && confirmed >= event.getCapacity()) {
            throw ApiException.conflict("O evento está lotado.");
        }

        Registration reg = existing != null ? existing : new Registration();
        reg.setEvent(event);
        reg.setName(name);
        reg.setEmail(email);
        reg.setUser(userId != null ? userRepository.getReferenceById(userId) : reg.getUser());
        reg.setStatus(RegistrationStatus.CONFIRMED);
        reg.setCheckedInAt(null);
        return registrationRepository.save(reg);
    }

    private Event lockEvent(Long eventId) {
        Event event = eventRepository.findForUpdateById(eventId)
                .orElseThrow(() -> ApiException.notFound("Evento não encontrado."));
        CurrentUser.assertSameTenant(event.getTenantId(), "Evento não encontrado.");
        return event;
    }

    private Registration getEntity(Long eventId, Long registrationId, Permission required) {
        eventAccess.require(eventService.getEntity(eventId), required);
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        CurrentUser.assertSameTenant(reg.getTenantId(), NOT_FOUND);
        if (!reg.getEvent().getId().equals(eventId)) {
            throw ApiException.notFound(NOT_FOUND);
        }
        return reg;
    }
}
