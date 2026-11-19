package com.eventhub.team;

import com.eventhub.access.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class TeamDtos {

    private TeamDtos() {
    }

    public record TeamFunctionResponse(Long id, String name, String description,
                                       List<Permission> permissions, long memberCount) {
        public static TeamFunctionResponse from(TeamFunction f, long memberCount) {
            return new TeamFunctionResponse(f.getId(), f.getName(), f.getDescription(),
                    f.getPermissions().stream().sorted(Comparator.comparing(Enum::ordinal)).toList(), memberCount);
        }
    }

    public record TeamFunctionRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 60) String name,
            @Size(max = 255) String description,
            @NotNull(message = "Informe as permissões") Set<Permission> permissions) {
    }

    /** Uma linha da equipe ao salvar: quem e com qual função. */
    public record TeamMemberInput(
            @NotNull(message = "Escolha a pessoa") Long userId,
            @NotNull(message = "Escolha a função") Long functionId) {
    }

    public record TeamMemberResponse(Long id, Long userId, String userName, String userEmail,
                                     Long functionId, String functionName) {
        public static TeamMemberResponse from(EventTeamMember m) {
            return new TeamMemberResponse(m.getId(), m.getUser().getId(), m.getUser().getName(),
                    m.getUser().getEmail(), m.getFunction().getId(), m.getFunction().getName());
        }
    }
}
