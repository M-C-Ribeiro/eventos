package com.eventhub.auth;

import com.eventhub.access.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** Cria uma nova organização (tenant) e o seu primeiro usuário administrador. */
    public record RegisterRequest(
            @NotBlank(message = "Informe o nome da organização") @Size(max = 120) String organizationName,
            @NotBlank(message = "Informe o identificador")
            @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Use apenas letras minúsculas, números e hífens")
            @Size(min = 3, max = 60, message = "Entre 3 e 60 caracteres") String organizationSlug,
            @NotBlank(message = "Informe seu nome") @Size(max = 120) String name,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @NotBlank(message = "Informe a senha") @Size(min = 8, max = 100, message = "A senha deve ter ao menos 8 caracteres") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe a organização") String organizationSlug,
            @NotBlank(message = "Informe o e-mail") String email,
            @NotBlank(message = "Informe a senha") String password) {
    }

    public record TenantInfo(Long id, String name, String slug) {
    }

    public record MeResponse(Long id, String name, String email, List<String> roles,
                             Set<Permission> permissions, TenantInfo tenant) {
    }

    public record AuthResponse(String token, long expiresIn, MeResponse user) {
    }
}
