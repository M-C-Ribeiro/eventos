package com.eventhub.team;

import com.eventhub.common.ApiException;
import com.eventhub.demand.DemandAssignmentRepository;
import com.eventhub.event.Event;
import com.eventhub.security.CurrentUser;
import com.eventhub.team.TeamDtos.TeamMemberInput;
import com.eventhub.team.TeamDtos.TeamMemberResponse;
import com.eventhub.user.User;
import com.eventhub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Monta e consulta a equipe de um evento. As checagens de permissão ficam no EventService. */
@Service
public class EventTeamService {

    private final EventTeamMemberRepository memberRepository;
    private final TeamFunctionRepository functionRepository;
    private final UserRepository userRepository;
    private final DemandAssignmentRepository demandAssignmentRepository;

    public EventTeamService(EventTeamMemberRepository memberRepository, TeamFunctionRepository functionRepository,
                            UserRepository userRepository, DemandAssignmentRepository demandAssignmentRepository) {
        this.memberRepository = memberRepository;
        this.functionRepository = functionRepository;
        this.userRepository = userRepository;
        this.demandAssignmentRepository = demandAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> list(Long eventId) {
        return memberRepository.findByEventIdOrderByUserNameAsc(eventId).stream()
                .map(TeamMemberResponse::from).toList();
    }

    /**
     * Substitui a equipe do evento pela lista informada: atualiza a função de quem já estava,
     * adiciona quem é novo e remove quem saiu.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void replace(Event event, List<TeamMemberInput> inputs) {
        Set<Long> userIds = new HashSet<>();
        for (TeamMemberInput in : inputs) {
            if (!userIds.add(in.userId())) {
                throw ApiException.badRequest("Cada pessoa só pode aparecer uma vez na equipe.");
            }
        }

        Map<Long, EventTeamMember> current = memberRepository.findByEventIdOrderByUserNameAsc(event.getId()).stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), Function.identity()));

        Map<Long, User> users = loadAll(userRepository.findAllById(userIds), User::getId, User::getTenantId,
                userIds, "Pessoa não encontrada na organização.");
        Set<Long> functionIds = inputs.stream().map(TeamMemberInput::functionId).collect(Collectors.toSet());
        Map<Long, TeamFunction> functions = loadAll(functionRepository.findAllById(functionIds), TeamFunction::getId,
                TeamFunction::getTenantId, functionIds, "Função não encontrada.");

        for (TeamMemberInput in : inputs) {
            EventTeamMember member = current.remove(in.userId());
            if (member == null) {
                User user = users.get(in.userId());
                if (!user.isActive()) {
                    throw ApiException.badRequest(user.getName() + " está desativado e não pode entrar na equipe.");
                }
                member = new EventTeamMember();
                member.setEvent(event);
                member.setUser(user);
            }
            member.setFunction(functions.get(in.functionId()));
            memberRepository.save(member);
        }
        // Quem sobrou em "current" não está mais na lista: sai da equipe.
        if (!current.isEmpty()) {
            demandAssignmentRepository.deleteUserAssignmentsInEvent(event.getId(), current.keySet());
        }
        memberRepository.deleteAll(current.values());
    }

    private static <T> Map<Long, T> loadAll(List<T> found, Function<T, Long> id, Function<T, Long> tenant,
                                            Set<Long> expected, String notFound) {
        found.forEach(item -> CurrentUser.assertSameTenant(tenant.apply(item), notFound));
        Map<Long, T> map = found.stream().collect(Collectors.toMap(id, Function.identity()));
        if (!map.keySet().containsAll(expected)) {
            throw ApiException.badRequest(notFound);
        }
        return map;
    }
}
