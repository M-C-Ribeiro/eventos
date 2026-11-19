package com.eventhub.event;

import com.eventhub.access.Permission;
import com.eventhub.common.ApiException;
import com.eventhub.event.EventDtos.EventRequest;
import com.eventhub.event.EventDtos.EventResponse;
import com.eventhub.registration.RegistrationRepository;
import com.eventhub.registration.RegistrationStatus;
import com.eventhub.demand.DemandAssignmentRepository;
import com.eventhub.demand.DemandCompletionRepository;
import com.eventhub.demand.DemandRepository;
import com.eventhub.meeting.MeetingAttendanceRepository;
import com.eventhub.meeting.PrepMeetingRepository;
import com.eventhub.schedule.ScheduleItemRepository;
import com.eventhub.security.AuthUser;
import com.eventhub.security.CurrentUser;
import com.eventhub.team.EventTeamMember;
import com.eventhub.team.EventTeamMemberRepository;
import com.eventhub.team.EventTeamService;
import com.eventhub.team.TeamDtos.TeamMemberInput;
import com.eventhub.team.TeamDtos.TeamMemberResponse;
import com.eventhub.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EventService {

    private static final String NOT_FOUND = "Evento não encontrado.";

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventTeamMemberRepository teamRepository;
    private final EventTeamService teamService;
    private final EventAccess eventAccess;
    private final ScheduleItemRepository scheduleRepository;
    private final PrepMeetingRepository meetingRepository;
    private final MeetingAttendanceRepository attendanceRepository;
    private final DemandRepository demandRepository;
    private final DemandAssignmentRepository demandAssignmentRepository;
    private final DemandCompletionRepository demandCompletionRepository;

    public EventService(EventRepository eventRepository, RegistrationRepository registrationRepository,
                        UserRepository userRepository, EventTeamMemberRepository teamRepository,
                        EventTeamService teamService, EventAccess eventAccess,
                        ScheduleItemRepository scheduleRepository, PrepMeetingRepository meetingRepository,
                        MeetingAttendanceRepository attendanceRepository, DemandRepository demandRepository,
                        DemandAssignmentRepository demandAssignmentRepository,
                        DemandCompletionRepository demandCompletionRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamService = teamService;
        this.eventAccess = eventAccess;
        this.scheduleRepository = scheduleRepository;
        this.meetingRepository = meetingRepository;
        this.attendanceRepository = attendanceRepository;
        this.demandRepository = demandRepository;
        this.demandAssignmentRepository = demandAssignmentRepository;
        this.demandCompletionRepository = demandCompletionRepository;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list(EventStatus status, String search, boolean upcomingOnly) {
        AuthUser me = CurrentUser.get();

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (!me.has(Permission.EVENT_VIEW_ALL)) {
                // Sem permissão para ver tudo: só eventos publicados/encerrados (se puder ver eventos),
                // os que a pessoa criou e os em que ela está na equipe.
                Subquery<Long> team = query.subquery(Long.class);
                Root<EventTeamMember> member = team.from(EventTeamMember.class);
                team.select(member.<Long>get("id"))
                        .where(cb.equal(member.get("event"), root),
                                cb.equal(member.get("user").get("id"), me.id()));

                List<Predicate> visible = new ArrayList<>();
                visible.add(cb.equal(root.get("organizer").get("id"), me.id()));
                visible.add(cb.exists(team));
                if (me.has(Permission.EVENT_VIEW)) {
                    visible.add(cb.notEqual(root.get("status"), EventStatus.DRAFT));
                }
                predicates.add(cb.or(visible.toArray(new Predicate[0])));
            }
            if (upcomingOnly) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("endAt"), LocalDateTime.now()));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("location"), "")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Event> events = eventRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "startAt"));
        Map<Long, Long> counts = confirmedCounts(events.stream().map(Event::getId).toList());
        return events.stream().map(e -> EventResponse.from(e, counts.getOrDefault(e.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long id) {
        return detail(getVisibleEntity(id));
    }

    @Transactional
    public EventResponse create(EventRequest req) {
        validate(req);
        AuthUser me = CurrentUser.get();
        Event event = new Event();
        apply(event, req);
        event.setStatus(req.status() != null ? req.status() : EventStatus.DRAFT);
        event.setOrganizer(userRepository.getReferenceById(me.id()));
        Event saved = eventRepository.save(event);
        if (req.team() != null) {
            // Quem cria é o responsável e, portanto, pode montar a equipe.
            teamService.replace(saved, req.team());
        }
        return detail(eventRepository.findWithOrganizerById(saved.getId()).orElseThrow());
    }

    @Transactional
    public EventResponse update(Long id, EventRequest req) {
        validate(req);
        Event event = getEntity(id);
        eventAccess.require(event, Permission.EVENT_EDIT);
        if (req.team() != null) {
            eventAccess.require(event, Permission.TEAM_MANAGE);
        }
        long confirmed = registrationRepository.countByEventIdAndStatusNot(id, RegistrationStatus.CANCELLED);
        if (req.capacity() != null && req.capacity() < confirmed) {
            throw ApiException.badRequest("A capacidade não pode ser menor que o número de inscritos (" + confirmed + ").");
        }
        apply(event, req);
        if (req.status() != null) {
            event.setStatus(req.status());
        }
        if (req.team() != null) {
            teamService.replace(event, req.team());
        }
        return detail(event);
    }

    /** Altera só a equipe (para quem pode gerenciar a equipe, mas não editar o evento). */
    @Transactional
    public List<TeamMemberResponse> replaceTeam(Long id, List<TeamMemberInput> members) {
        Event event = getEntity(id);
        eventAccess.require(event, Permission.TEAM_MANAGE);
        teamService.replace(event, members);
        return teamService.list(id);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> team(Long id) {
        getVisibleEntity(id);
        return teamService.list(id);
    }

    @Transactional
    public void delete(Long id) {
        Event event = getEntity(id);
        eventAccess.require(event, Permission.EVENT_DELETE);
        teamRepository.deleteByEventId(event.getId());
        scheduleRepository.deleteByEventId(event.getId());
        attendanceRepository.deleteByEventId(event.getId());
        meetingRepository.deleteByEventId(event.getId());
        demandCompletionRepository.deleteByEventId(event.getId());
        demandAssignmentRepository.deleteByEventId(event.getId());
        demandRepository.deleteByEventId(event.getId());
        registrationRepository.deleteByEventId(event.getId());
        eventRepository.delete(event);
    }

    /** Busca o evento garantindo que pertence ao tenant atual (sem checar visibilidade). */
    public Event getEntity(Long id) {
        Event event = eventRepository.findWithOrganizerById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        CurrentUser.assertSameTenant(event.getTenantId(), NOT_FOUND);
        return event;
    }

    /** Como getEntity, mas responde 404 se o usuário não pode ver o evento. */
    public Event getVisibleEntity(Long id) {
        Event event = getEntity(id);
        if (!eventAccess.canView(event)) {
            throw ApiException.notFound(NOT_FOUND);
        }
        return event;
    }

    private EventResponse detail(Event event) {
        long confirmed = registrationRepository.countByEventIdAndStatusNot(event.getId(), RegistrationStatus.CANCELLED);
        return EventResponse.from(event, confirmed, eventAccess.permissionsOn(event), teamService.list(event.getId()));
    }

    private Map<Long, Long> confirmedCounts(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : registrationRepository.countActiveByEventIds(eventIds, RegistrationStatus.CANCELLED)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private static void validate(EventRequest req) {
        if (!req.endAt().isAfter(req.startAt())) {
            throw ApiException.badRequest("O término deve ser depois do início.");
        }
    }

    private static void apply(Event event, EventRequest req) {
        event.setTitle(req.title().trim());
        event.setDescription(blankToNull(req.description()));
        event.setLocation(blankToNull(req.location()));
        event.setStartAt(req.startAt());
        event.setEndAt(req.endAt());
        event.setCapacity(req.capacity());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
