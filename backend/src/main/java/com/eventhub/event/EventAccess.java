package com.eventhub.event;

import com.eventhub.access.Permission;
import com.eventhub.common.ApiException;
import com.eventhub.security.AuthUser;
import com.eventhub.security.CurrentUser;
import com.eventhub.team.EventTeamMemberRepository;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Decide o que o usuário atual pode fazer em um evento específico.
 * <p>
 * Permissões efetivas no evento = permissões de evento dos perfis do usuário (valem para todos os eventos)
 * + todas as permissões de evento, se ele é o responsável (quem criou)
 * + as permissões da função que ele exerce na equipe desse evento.
 */
@Component("eventAccess")
public class EventAccess {

    private final EventTeamMemberRepository teamRepository;

    public EventAccess(EventTeamMemberRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Set<Permission> permissionsOn(Event event) {
        AuthUser me = CurrentUser.get();
        Set<Permission> result = EnumSet.noneOf(Permission.class);
        for (Permission p : me.permissions()) {
            if (p.eventScoped() || p == Permission.EVENT_DELETE) {
                result.add(p);
            }
        }
        if (isOrganizer(event, me)) {
            result.addAll(Permission.organizerSet());
        }
        teamRepository.findByEventIdAndUserId(event.getId(), me.id())
                .ifPresent(member -> result.addAll(member.getFunction().getPermissions()));
        return result;
    }

    public boolean canView(Event event) {
        AuthUser me = CurrentUser.get();
        return me.has(Permission.EVENT_VIEW_ALL)
                || isOrganizer(event, me)
                || (me.has(Permission.EVENT_VIEW) && event.getStatus() != EventStatus.DRAFT)
                || teamRepository.existsByEventIdAndUserId(event.getId(), me.id());
    }

    /** Exige a permissão no evento. Quem nem pode ver o evento recebe 404, para não revelar que ele existe. */
    public void require(Event event, Permission permission) {
        if (permissionsOn(event).contains(permission)) {
            return;
        }
        if (!canView(event)) {
            throw ApiException.notFound("Evento não encontrado.");
        }
        throw ApiException.forbidden("Você não tem permissão para esta ação neste evento.");
    }

    /**
     * Informações internas do evento (ex.: reuniões preparatórias) não são públicas: só as vê
     * quem está na equipe, quem pode editar ou gerenciar a equipe do evento, ou quem vê todos os eventos.
     */
    public boolean canViewInternal(Event event) {
        AuthUser me = CurrentUser.get();
        if (me.has(Permission.EVENT_VIEW_ALL)) {
            return true;
        }
        Set<Permission> perms = permissionsOn(event);
        return perms.contains(Permission.EVENT_EDIT) || perms.contains(Permission.TEAM_MANAGE)
                || teamRepository.existsByEventIdAndUserId(event.getId(), me.id());
    }

    public void requireInternalView(Event event) {
        if (canViewInternal(event)) {
            return;
        }
        if (!canView(event)) {
            throw ApiException.notFound("Evento não encontrado.");
        }
        throw ApiException.forbidden("Somente a equipe do evento tem acesso a esta informação.");
    }

    /** Usado em @PreAuthorize: a pessoa gerencia a equipe de pelo menos um evento? */
    public boolean managesAnyTeam() {
        return teamRepository.countMembershipsWithPermission(CurrentUser.get().id(), Permission.TEAM_MANAGE) > 0;
    }

    private static boolean isOrganizer(Event event, AuthUser me) {
        return event.getOrganizer() != null && me.id().equals(event.getOrganizer().getId());
    }
}
