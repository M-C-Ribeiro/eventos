package com.eventhub.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class RegistrationDtos {

    private RegistrationDtos() {
    }

    public record RegistrationRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String name,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email) {
    }

    public record RegistrationResponse(
            Long id,
            Long eventId,
            Long userId,
            String name,
            String email,
            RegistrationStatus status,
            Instant registeredAt,
            Instant checkedInAt) {

        public static RegistrationResponse from(Registration r) {
            return new RegistrationResponse(r.getId(), r.getEvent().getId(),
                    r.getUser() != null ? r.getUser().getId() : null,
                    r.getName(), r.getEmail(), r.getStatus(), r.getRegisteredAt(), r.getCheckedInAt());
        }
    }
}
