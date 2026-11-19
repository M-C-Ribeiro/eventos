package com.eventhub.event;

import com.eventhub.access.Permission;
import com.eventhub.team.TeamDtos.TeamMemberInput;
import com.eventhub.team.TeamDtos.TeamMemberResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class EventDtos {

    private EventDtos() {
    }

    /**
     * Dados do evento. {@code team} é opcional: quando enviado, substitui a equipe inteira
     * (exige permissão de gerenciar equipe); quando nulo, a equipe não é alterada.
     */
    public record EventRequest(
            @NotBlank(message = "Informe o título") @Size(max = 160) String title,
            @Size(max = 4000) String description,
            @Size(max = 255) String location,
            @NotNull(message = "Informe o início") LocalDateTime startAt,
            @NotNull(message = "Informe o término") LocalDateTime endAt,
            @Min(value = 1, message = "A capacidade deve ser no mínimo 1") Integer capacity,
            EventStatus status,
            @Valid List<TeamMemberInput> team) {
    }

    public record TeamRequest(@NotNull(message = "Informe a equipe") @Valid List<TeamMemberInput> members) {
    }

    public record OrganizerSummary(Long id, String name) {
    }

    /**
     * {@code myPermissions} e {@code team} só vêm preenchidos no detalhe de um evento.
     */
    public record EventResponse(
            Long id,
            String title,
            String description,
            String location,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Integer capacity,
            EventStatus status,
            OrganizerSummary organizer,
            long confirmedCount,
            Instant createdAt,
            List<Permission> myPermissions,
            List<TeamMemberResponse> team) {

        public static EventResponse from(Event e, long confirmedCount) {
            return from(e, confirmedCount, null, null);
        }

        public static EventResponse from(Event e, long confirmedCount, Set<Permission> myPermissions,
                                         List<TeamMemberResponse> team) {
            return new EventResponse(e.getId(), e.getTitle(), e.getDescription(), e.getLocation(),
                    e.getStartAt(), e.getEndAt(), e.getCapacity(), e.getStatus(),
                    new OrganizerSummary(e.getOrganizer().getId(), e.getOrganizer().getName()),
                    confirmedCount, e.getCreatedAt(),
                    myPermissions == null ? null
                            : myPermissions.stream().sorted(Comparator.comparing(Enum::ordinal)).toList(),
                    team);
        }
    }
}
