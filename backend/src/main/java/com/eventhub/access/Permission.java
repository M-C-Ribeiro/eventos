package com.eventhub.access;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catálogo de permissões do sistema (RBAC).
 * <p>
 * Permissões são atribuídas a perfis de acesso (globais na organização). As marcadas como
 * {@code eventScoped} também podem ser concedidas por uma função de equipe, valendo apenas
 * no evento em que a pessoa faz parte da equipe.
 */
public enum Permission {

    EVENT_VIEW("Ver eventos publicados", false),
    EVENT_VIEW_ALL("Ver todos os eventos, incluindo rascunhos", false),
    EVENT_CREATE("Criar eventos", false),
    EVENT_EDIT("Editar dados do evento", true),
    EVENT_DELETE("Excluir eventos", false),
    REGISTRATION_VIEW("Ver inscrições", true),
    REGISTRATION_MANAGE("Adicionar e cancelar inscrições", true),
    CHECKIN("Fazer check-in", true),
    TEAM_MANAGE("Gerenciar a equipe do evento", true),
    TEAM_FUNCTION_MANAGE("Gerenciar o catálogo de funções de equipe", false),
    USER_MANAGE("Gerenciar usuários", false),
    ROLE_MANAGE("Gerenciar perfis de acesso", false),
    ORGANIZATION_MANAGE("Editar dados da organização", false);

    private final String label;
    private final boolean eventScoped;

    Permission(String label, boolean eventScoped) {
        this.label = label;
        this.eventScoped = eventScoped;
    }

    public String label() {
        return label;
    }

    public boolean eventScoped() {
        return eventScoped;
    }

    /** Permissões que podem ser concedidas por função de equipe dentro de um evento. */
    public static Set<Permission> eventScopedSet() {
        return Arrays.stream(values()).filter(Permission::eventScoped)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));
    }

    /** O responsável (quem criou o evento) tem todas as permissões de evento, inclusive excluir. */
    public static Set<Permission> organizerSet() {
        Set<Permission> set = eventScopedSet();
        set.add(EVENT_DELETE);
        return set;
    }
}
