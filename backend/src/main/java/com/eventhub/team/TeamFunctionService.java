package com.eventhub.team;

import com.eventhub.access.Permission;
import com.eventhub.common.ApiException;
import com.eventhub.demand.DemandAssignmentRepository;
import com.eventhub.security.CurrentUser;
import com.eventhub.team.TeamDtos.TeamFunctionRequest;
import com.eventhub.team.TeamDtos.TeamFunctionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamFunctionService {

    private static final String NOT_FOUND = "Função não encontrada.";

    private final TeamFunctionRepository functionRepository;
    private final EventTeamMemberRepository memberRepository;
    private final DemandAssignmentRepository demandAssignmentRepository;

    public TeamFunctionService(TeamFunctionRepository functionRepository, EventTeamMemberRepository memberRepository,
                               DemandAssignmentRepository demandAssignmentRepository) {
        this.functionRepository = functionRepository;
        this.memberRepository = memberRepository;
        this.demandAssignmentRepository = demandAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamFunctionResponse> list() {
        return functionRepository.findAllByOrderByNameAsc().stream()
                .map(f -> TeamFunctionResponse.from(f, memberRepository.countByFunctionId(f.getId())))
                .toList();
    }

    @Transactional
    public TeamFunctionResponse create(TeamFunctionRequest req) {
        String name = req.name().trim();
        if (functionRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("Já existe uma função com este nome.");
        }
        TeamFunction f = functionRepository.save(new TeamFunction(name, blankToNull(req.description()),
                validated(req.permissions())));
        return TeamFunctionResponse.from(f, 0);
    }

    @Transactional
    public TeamFunctionResponse update(Long id, TeamFunctionRequest req) {
        TeamFunction f = getEntity(id);
        String name = req.name().trim();
        if (functionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict("Já existe uma função com este nome.");
        }
        f.setName(name);
        f.setDescription(blankToNull(req.description()));
        f.setPermissions(new HashSet<>(validated(req.permissions())));
        return TeamFunctionResponse.from(f, memberRepository.countByFunctionId(id));
    }

    @Transactional
    public void delete(Long id) {
        TeamFunction f = getEntity(id);
        if (memberRepository.existsByFunctionId(id)) {
            throw ApiException.conflict("Esta função está em uso em equipes de eventos. Troque a função dessas pessoas antes de excluir.");
        }
        // Demandas atribuídas a esta função deixam de estar atribuídas a ela.
        demandAssignmentRepository.deleteByFunctionId(id);
        functionRepository.delete(f);
    }

    /** Só permissões de evento fazem sentido numa função de equipe. */
    private static Set<Permission> validated(Set<Permission> permissions) {
        Set<Permission> invalid = permissions.stream().filter(p -> !p.eventScoped()).collect(Collectors.toSet());
        if (!invalid.isEmpty()) {
            throw ApiException.badRequest("Estas permissões não podem ser dadas por função de equipe: " + invalid);
        }
        return permissions;
    }

    private TeamFunction getEntity(Long id) {
        TeamFunction f = functionRepository.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        CurrentUser.assertSameTenant(f.getTenantId(), NOT_FOUND);
        return f;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
