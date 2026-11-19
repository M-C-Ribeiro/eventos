package com.eventhub.meeting;

import com.eventhub.access.Permission;
import com.eventhub.common.ApiException;
import com.eventhub.event.Event;
import com.eventhub.event.EventAccess;
import com.eventhub.event.EventService;
import com.eventhub.meeting.MeetingDtos.*;
import com.eventhub.security.CurrentUser;
import com.eventhub.team.EventTeamMember;
import com.eventhub.team.EventTeamMemberRepository;
import com.eventhub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reuniões preparatórias e lista de presença da equipe.
 * <p>
 * Ver: equipe do evento e quem pode editar/gerenciar a equipe (informação interna, ver EventAccess).
 * Alterar reuniões e marcar presença: permissão de editar o evento.
 */
@Service
public class PrepMeetingService {

    private static final String NOT_FOUND = "Reunião não encontrada.";

    private final PrepMeetingRepository meetingRepository;
    private final MeetingAttendanceRepository attendanceRepository;
    private final EventTeamMemberRepository teamRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final EventAccess eventAccess;

    public PrepMeetingService(PrepMeetingRepository meetingRepository, MeetingAttendanceRepository attendanceRepository,
                              EventTeamMemberRepository teamRepository, UserRepository userRepository,
                              EventService eventService, EventAccess eventAccess) {
        this.meetingRepository = meetingRepository;
        this.attendanceRepository = attendanceRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.eventAccess = eventAccess;
    }

    // ---------- reuniões ----------

    @Transactional(readOnly = true)
    public List<MeetingResponse> list(Long eventId) {
        Event event = eventService.getEntity(eventId);
        eventAccess.requireInternalView(event);

        Set<Long> teamIds = teamUserIds(eventId);
        Map<Long, Long> present = attendanceRepository.findPresentByEventId(eventId).stream()
                .filter(a -> teamIds.contains(a.getUser().getId()))
                .collect(Collectors.groupingBy(a -> a.getMeeting().getId(), Collectors.counting()));

        return meetingRepository.findByEventIdOrderByDateAscTimeAscIdAsc(eventId).stream()
                .map(m -> MeetingResponse.from(m, present.getOrDefault(m.getId(), 0L), teamIds.size()))
                .toList();
    }

    @Transactional
    public MeetingResponse create(Long eventId, MeetingRequest req) {
        Event event = editableEvent(eventId);
        PrepMeeting meeting = new PrepMeeting();
        meeting.setEvent(event);
        apply(meeting, req);
        return MeetingResponse.from(meetingRepository.save(meeting), 0, teamUserIds(eventId).size());
    }

    @Transactional
    public MeetingResponse update(Long eventId, Long meetingId, MeetingRequest req) {
        editableEvent(eventId);
        PrepMeeting meeting = getMeeting(eventId, meetingId);
        apply(meeting, req);
        Set<Long> teamIds = teamUserIds(eventId);
        long present = attendanceRepository.findByMeetingId(meetingId).stream()
                .filter(a -> a.isPresent() && teamIds.contains(a.getUser().getId())).count();
        return MeetingResponse.from(meeting, present, teamIds.size());
    }

    @Transactional
    public void delete(Long eventId, Long meetingId) {
        editableEvent(eventId);
        PrepMeeting meeting = getMeeting(eventId, meetingId);
        attendanceRepository.deleteByMeetingId(meetingId);
        meetingRepository.delete(meeting);
    }

    // ---------- lista de presença ----------

    @Transactional(readOnly = true)
    public List<AttendanceRow> attendance(Long eventId, Long meetingId) {
        Event event = eventService.getEntity(eventId);
        eventAccess.requireInternalView(event);
        getMeeting(eventId, meetingId);
        return buildRows(eventId, meetingId);
    }

    @Transactional
    public AttendanceRow mark(Long eventId, Long meetingId, Long userId, AttendanceRequest req) {
        editableEvent(eventId);
        PrepMeeting meeting = getMeeting(eventId, meetingId);

        MeetingAttendance record = attendanceRepository.findByMeetingIdAndUserId(meetingId, userId).orElse(null);
        Optional<EventTeamMember> member = teamRepository.findByEventIdAndUserId(eventId, userId);
        if (record == null && member.isEmpty()) {
            throw ApiException.badRequest("Esta pessoa não faz parte da equipe do evento.");
        }
        String justification = req.justification() == null || req.justification().isBlank()
                ? null : req.justification().trim();
        if (req.present() && justification != null) {
            throw ApiException.badRequest("A justificativa só pode ser registrada para quem está ausente.");
        }

        if (record == null) {
            record = new MeetingAttendance();
            record.setMeeting(meeting);
            record.setUser(userRepository.getReferenceById(userId));
        }
        record.setPresent(req.present());
        record.setJustification(req.present() ? null : justification);
        record.setUpdatedAt(Instant.now());
        attendanceRepository.save(record);

        return buildRows(eventId, meetingId).stream()
                .filter(r -> r.userId().equals(userId)).findFirst().orElseThrow();
    }

    /** Equipe atual (em ordem de nome) + quem tem registro de presença mas saiu da equipe. */
    private List<AttendanceRow> buildRows(Long eventId, Long meetingId) {
        Map<Long, MeetingAttendance> records = attendanceRepository.findByMeetingId(meetingId).stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), Function.identity()));

        List<AttendanceRow> rows = new ArrayList<>();
        for (EventTeamMember m : teamRepository.findByEventIdOrderByUserNameAsc(eventId)) {
            MeetingAttendance a = records.remove(m.getUser().getId());
            rows.add(new AttendanceRow(m.getUser().getId(), m.getUser().getName(), m.getFunction().getName(),
                    a != null && a.isPresent(), a == null ? null : a.getJustification(), true));
        }
        records.values().stream()
                .sorted(Comparator.comparing(a -> a.getUser().getName()))
                .forEach(a -> rows.add(new AttendanceRow(a.getUser().getId(), a.getUser().getName(), null,
                        a.isPresent(), a.getJustification(), false)));
        return rows;
    }

    private Set<Long> teamUserIds(Long eventId) {
        return teamRepository.findByEventIdOrderByUserNameAsc(eventId).stream()
                .map(m -> m.getUser().getId()).collect(Collectors.toSet());
    }

    private Event editableEvent(Long eventId) {
        Event event = eventService.getEntity(eventId);
        eventAccess.require(event, Permission.EVENT_EDIT);
        return event;
    }

    private PrepMeeting getMeeting(Long eventId, Long meetingId) {
        PrepMeeting m = meetingRepository.findById(meetingId).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        CurrentUser.assertSameTenant(m.getTenantId(), NOT_FOUND);
        if (!m.getEvent().getId().equals(eventId)) {
            throw ApiException.notFound(NOT_FOUND);
        }
        return m;
    }

    private static void apply(PrepMeeting m, MeetingRequest req) {
        m.setTitle(req.title().trim());
        m.setDate(req.date());
        m.setTime(req.time());
        m.setLocation(req.location().trim());
    }
}
