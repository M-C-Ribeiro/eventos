package com.eventhub.demand;

import com.eventhub.demand.DemandDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Permissões por evento são checadas no DemandService. */
@RestController
@RequestMapping("/api/events/{eventId}/demands")
public class DemandController {

    private final DemandService service;

    public DemandController(DemandService service) {
        this.service = service;
    }

    @GetMapping
    public List<DemandResponse> list(@PathVariable Long eventId) {
        return service.list(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemandResponse create(@PathVariable Long eventId, @Valid @RequestBody DemandRequest request) {
        return service.create(eventId, request);
    }

    @PutMapping("/{demandId}")
    public DemandResponse update(@PathVariable Long eventId, @PathVariable Long demandId,
                                 @Valid @RequestBody DemandRequest request) {
        return service.update(eventId, demandId, request);
    }

    @DeleteMapping("/{demandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long eventId, @PathVariable Long demandId) {
        service.delete(eventId, demandId);
    }

    /** Concluído (completed=true) ou Pendente (completed=false) para um dos responsáveis. */
    @PutMapping("/{demandId}/completion/{userId}")
    public DemandResponse setCompletion(@PathVariable Long eventId, @PathVariable Long demandId, @PathVariable Long userId,
                                        @Valid @RequestBody CompletionRequest request) {
        return service.setCompletion(eventId, demandId, userId, request);
    }

    @PutMapping("/{demandId}/assignments")
    public DemandResponse assign(@PathVariable Long eventId, @PathVariable Long demandId,
                                 @Valid @RequestBody AssignRequest request) {
        return service.assign(eventId, demandId, request);
    }
}
