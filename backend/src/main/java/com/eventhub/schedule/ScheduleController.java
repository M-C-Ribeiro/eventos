package com.eventhub.schedule;

import com.eventhub.schedule.ScheduleDtos.ScheduleItemRequest;
import com.eventhub.schedule.ScheduleDtos.ScheduleItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Permissões por evento (ver / editar) são checadas no ScheduleService. */
@RestController
@RequestMapping("/api/events/{eventId}/schedule")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScheduleItemResponse> list(@PathVariable Long eventId) {
        return service.list(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleItemResponse create(@PathVariable Long eventId, @Valid @RequestBody ScheduleItemRequest request) {
        return service.create(eventId, request);
    }

    @PutMapping("/{itemId}")
    public ScheduleItemResponse update(@PathVariable Long eventId, @PathVariable Long itemId,
                                       @Valid @RequestBody ScheduleItemRequest request) {
        return service.update(eventId, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long eventId, @PathVariable Long itemId) {
        service.delete(eventId, itemId);
    }
}
