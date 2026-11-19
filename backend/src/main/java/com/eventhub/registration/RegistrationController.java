package com.eventhub.registration;

import com.eventhub.registration.RegistrationDtos.RegistrationRequest;
import com.eventhub.registration.RegistrationDtos.RegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Permissões por evento (ver inscrições, gerenciar, check-in) são checadas no RegistrationService. */
@RestController
@RequestMapping("/api/events/{eventId}/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<RegistrationResponse> list(@PathVariable Long eventId) {
        return registrationService.list(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse create(@PathVariable Long eventId, @Valid @RequestBody RegistrationRequest request) {
        return registrationService.create(eventId, request);
    }


    @PostMapping("/{registrationId}/check-in")
    public RegistrationResponse checkIn(@PathVariable Long eventId, @PathVariable Long registrationId) {
        return registrationService.checkIn(eventId, registrationId);
    }

    @DeleteMapping("/{registrationId}/check-in")
    public RegistrationResponse undoCheckIn(@PathVariable Long eventId, @PathVariable Long registrationId) {
        return registrationService.undoCheckIn(eventId, registrationId);
    }

    @PostMapping("/{registrationId}/cancel")
    public RegistrationResponse cancel(@PathVariable Long eventId, @PathVariable Long registrationId) {
        return registrationService.cancel(eventId, registrationId);
    }
}
