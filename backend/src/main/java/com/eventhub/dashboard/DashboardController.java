package com.eventhub.dashboard;

import com.eventhub.event.EventAccess;
import com.eventhub.event.EventDtos.EventResponse;
import com.eventhub.event.EventRepository;
import com.eventhub.event.EventStatus;
import com.eventhub.registration.RegistrationRepository;
import com.eventhub.registration.RegistrationStatus;
import com.eventhub.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventAccess eventAccess;

    public DashboardController(EventRepository eventRepository, RegistrationRepository registrationRepository,
                               UserRepository userRepository, EventAccess eventAccess) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventAccess = eventAccess;
    }

    public record DashboardResponse(
            long upcomingEvents,
            long draftEvents,
            long activeRegistrations,
            long checkIns,
            long users,
            List<EventResponse> nextEvents) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        LocalDateTime now = LocalDateTime.now();
        List<EventResponse> next = eventRepository
                .findTop5ByStatusAndStartAtAfterOrderByStartAtAsc(EventStatus.PUBLISHED, now).stream()
                .filter(eventAccess::canView)
                .map(e -> EventResponse.from(e,
                        registrationRepository.countByEventIdAndStatusNot(e.getId(), RegistrationStatus.CANCELLED)))
                .toList();

        return new DashboardResponse(
                eventRepository.countByStatusAndStartAtAfter(EventStatus.PUBLISHED, now),
                eventRepository.countByStatus(EventStatus.DRAFT),
                registrationRepository.countByStatusNot(RegistrationStatus.CANCELLED),
                registrationRepository.countByStatus(RegistrationStatus.CHECKED_IN),
                userRepository.count(),
                next);
    }
}
