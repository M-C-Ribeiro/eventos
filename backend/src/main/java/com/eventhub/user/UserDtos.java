package com.eventhub.user;

import com.eventhub.access.AccessDtos.RoleSummary;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(Long id, String name, String email, List<RoleSummary> roles,
                               boolean active, Instant createdAt) {
        public static UserResponse from(User u) {
            List<RoleSummary> roles = u.getRoles().stream()
                    .map(RoleSummary::from)
                    .sorted(Comparator.comparing(RoleSummary::name))
                    .toList();
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), roles, u.isActive(), u.getCreatedAt());
        }
    }

    /** Dados mínimos para escolher pessoas (ex.: montar a equipe de um evento). */
    public record UserOption(Long id, String name, String email) {
        public static UserOption from(User u) {
            return new UserOption(u.getId(), u.getName(), u.getEmail());
        }
    }

    public record CreateUserRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String name,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @NotBlank(message = "Informe a senha") @Size(min = 8, max = 100, message = "A senha deve ter ao menos 8 caracteres") String password,
            @NotEmpty(message = "Escolha pelo menos um perfil") Set<Long> roleIds) {
    }

    public record UpdateUserRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String name,
            @NotEmpty(message = "Escolha pelo menos um perfil") Set<Long> roleIds,
            boolean active,
            @Size(min = 8, max = 100, message = "A senha deve ter ao menos 8 caracteres") String password) {
    }
}
