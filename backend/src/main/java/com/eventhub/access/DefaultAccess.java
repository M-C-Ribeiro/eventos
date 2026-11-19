package com.eventhub.access;

import java.util.EnumSet;
import java.util.Set;

import static com.eventhub.access.Permission.*;

/**
 * Perfis e funções criados para toda organização nova.
 * As migrações V2 aplicam os mesmos valores às organizações já existentes.
 */
public final class DefaultAccess {

    private DefaultAccess() {
    }

    public record Definition(String name, String description, Set<Permission> permissions) {
    }

    public static final Definition ADMIN = new Definition("Administrador",
            "Acesso total à organização.", EnumSet.allOf(Permission.class));

    public static final Set<Definition> ROLES = Set.of(
            new Definition("Organizador", "Cria e gerencia eventos, inscrições, check-in e equipes.",
                    EnumSet.of(EVENT_VIEW, EVENT_VIEW_ALL, EVENT_CREATE, EVENT_EDIT, EVENT_DELETE,
                            REGISTRATION_VIEW, REGISTRATION_MANAGE, CHECKIN, TEAM_MANAGE)),
            new Definition("Membro", "Vê os eventos publicados.", EnumSet.of(EVENT_VIEW)));

    public static final Set<Definition> TEAM_FUNCTIONS = Set.of(
            new Definition("Coordenação", "Coordena o evento: edita dados, inscrições, check-in e equipe.",
                    EnumSet.of(EVENT_EDIT, REGISTRATION_VIEW, REGISTRATION_MANAGE, CHECKIN, TEAM_MANAGE)),
            new Definition("Recepção", "Recebe os participantes e faz o check-in.",
                    EnumSet.of(REGISTRATION_VIEW, CHECKIN)),
            new Definition("Apoio", "Ajuda na execução; vê o evento mesmo antes de publicado.",
                    EnumSet.noneOf(Permission.class)));
}
