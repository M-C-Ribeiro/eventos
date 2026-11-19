package com.eventhub.security;

import com.eventhub.access.AccessRole;
import com.eventhub.access.Permission;
import com.eventhub.user.User;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Usuário autenticado na requisição atual (principal do Spring Security). */
public record AuthUser(Long id, Long tenantId, String name, String email,
                       List<String> roles, Set<Permission> permissions) {

    /** Os perfis do usuário precisam estar carregados. */
    public static AuthUser of(User user) {
        List<String> roleNames = user.getRoles().stream()
                .sorted(Comparator.comparing(AccessRole::getName))
                .map(AccessRole::getName)
                .toList();
        return new AuthUser(user.getId(), user.getTenantId(), user.getName(), user.getEmail(),
                roleNames, user.effectivePermissions());
    }

    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }
}
