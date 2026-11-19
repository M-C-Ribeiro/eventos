package com.eventhub.security;

import com.eventhub.common.ApiException;
import com.eventhub.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        throw ApiException.unauthorized("Não autenticado.");
    }

    /**
     * Segunda camada de proteção: garante que uma entidade carregada pertence ao tenant atual.
     * O Hibernate já filtra as consultas por tenant; isto cobre buscas por id.
     */
    public static void assertSameTenant(Long entityTenantId, String notFoundMessage) {
        if (!Objects.equals(entityTenantId, TenantContext.getTenantId())) {
            throw ApiException.notFound(notFoundMessage);
        }
    }
}
