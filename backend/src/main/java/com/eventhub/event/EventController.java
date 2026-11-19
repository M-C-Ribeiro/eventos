package com.eventhub.event;

import com.eventhub.event.EventDtos.EventRequest;
import com.eventhub.event.EventDtos.EventResponse;
import com.eventhub.event.EventDtos.TeamRequest;
import com.eventhub.team.TeamDtos.TeamMemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Permissões por evento (editar, excluir, equipe) são checadas no EventService,
 * pois dependem do responsável e da equipe de cada evento.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventResponse> list(@RequestParam(required = false) EventStatus status,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "false") boolean upcoming) {
        return eventService.list(status, q, upcoming);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EVENT_CREATE')")
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return eventService.create(request);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }

    @GetMapping("/{id}/team")
    public List<TeamMemberResponse> team(@PathVariable Long id) {
        return eventService.team(id);
    }

    @PutMapping("/{id}/team")
    public List<TeamMemberResponse> replaceTeam(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return eventService.replaceTeam(id, request.members());
    }
}
