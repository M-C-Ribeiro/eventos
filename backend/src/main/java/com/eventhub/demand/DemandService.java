package com.eventhub.demand;

import com.eventhub.access.Permission;
import com.eventhub.common.ApiException;
import com.eventhub.demand.DemandDtos.*;
import com.eventhub.event.Event;
import com.eventhub.event.EventAccess;
import com.eventhub.event.EventService;
import com.eventhub.security.CurrentUser;
import com.eventhub.team.EventTeamMember;
import com.eventhub.team.EventTeamMemberRepository;
import com.eventhub.team.TeamFunction;
import com.eventhub.team.TeamFunctionRepository;
import com.eventhub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Demandas do evento e suas atribuições.
 * <p>
 * Ver: equipe do evento e quem a gerencia (informação interna, ver EventAccess).
 * Cadastrar, alterar e atribuir: permissão de editar o evento.
 */
@Service
public class DemandService {

    private static final String NOT_FOUND = "Demanda não encontrada.";

    private final DemandRepository demandRepository;
    private final DemandAssignmentRepository assignmentRepository;
    private final DemandCompletionRepository completionRepository;
    private final UserRepository userRepository;
    private final EventTeamMemberRepository teamRepository;
    private final TeamFunctionRepository functionRepository;
    private final EventService eventService;
    private final EventAccess eventAccess;

    public DemandService(DemandRepository demandRepository, DemandAssignmentRepository assignmentRepository,
                         DemandCompletionRepository completionRepository, UserRepository userRepository,
                         EventTeamMemberRepository teamRepository, TeamFunctionRepository functionRepository,
                         EventService eventService, EventAccess eventAccess) {
        this.demandRepository = demandRepository;
        this.assignmentRepository = assignmentRepository;
        this.completionRepository = completionRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.functionRepository = functionRepository;
        this.eventService = eventService;
        this.eventAccess = eventAccess;
    }

    @Transactional(readOnly = true)
    public List<DemandResponse> list(Long eventId) {
        Event event = eventService.getEntity(eventId);
        eventAccess.requireInternalView(event);
        return buildResponses(eventId, demandRepository.findByEventIdOrderByDueDateAscIdAsc(eventId));
    }

    @Transactional
    public DemandResponse create(Long eventId, DemandRequest req) {
        Event event = editableEvent(eventId);
        Demand demand = new Demand();
        demand.setEvent(event);
        apply(demand, req);
        return single(eventId, demandRepository.save(demand));
    }

    @Transactional
    public DemandResponse update(Long eventId, Long demandId, DemandRequest req) {
        editableEvent(eventId);
        Demand demand = getDemand(eventId, demandId);
        apply(demand, req);
        return single(eventId, demand);
    }

    @Transactional
    public void delete(Long eventId, Long demandId) {
        editableEvent(eventId);
        Demand demand = getDemand(eventId, demandId);
        completionRepository.deleteByDemandId(demandId);
        assignmentRepository.deleteByDemandId(demandId);
        demandRepository.delete(demand);
    }

    /** Substitui as atribuições: funções do catálogo e/ou pessoas que estão na equipe do evento. */
    @Transactional
    public DemandResponse assign(Long eventId, Long demandId, AssignRequest req) {
        editableEvent(eventId);
        Demand demand = getDemand(eventId, demandId);

        List<TeamFunction> functions = functionRepository.findAllById(req.functionIds());
        functions.forEach(f -> CurrentUser.assertSameTenant(f.getTenantId(), "Função não encontrada."));
        if (functions.size() != req.functionIds().size()) {
            throw ApiException.badRequest("Função não encontrada.");
        }

        Map<Long, EventTeamMember> team = teamRepository.findByEventIdOrderByUserNameAsc(eventId).stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), Function.identity()));
        if (!team.keySet().containsAll(req.userIds())) {
            throw ApiException.badRequest("Só é possível atribuir a pessoas que estão na equipe do evento.");
        }

        assignmentRepository.deleteByDemandId(demandId);
        assignmentRepository.flush();
        functions.forEach(f -> assignmentRepository.save(DemandAssignment.ofFunction(demand, f)));
        req.userIds().forEach(id -> assignmentRepository.save(DemandAssignment.ofUser(demand, team.get(id).getUser())));
        assignmentRepository.flush();

        // Quem deixou de ser responsável perde a conclusão registrada (se voltar, começa pendente).
        DemandResponse updated = single(eventId, demand);
        Set<Long> still = updated.assignees().stream().map(Assignee::userId).collect(Collectors.toSet());
        List<DemandCompletion> stale = completionRepository.findByEventId(eventId).stream()
                .filter(c -> c.getDemand().getId().equals(demandId) && !still.contains(c.getUser().getId()))
                .toList();
        if (stale.isEmpty()) {
            return updated;
        }
        completionRepository.deleteAll(stale);
        completionRepository.flush();
        return single(eventId, demand);
    }

    /**
     * Marca a demanda como concluída ou pendente para um dos responsáveis.
     * Quem edita o evento marca para qualquer responsável; os demais, só para si mesmos.
     */
    @Transactional
    public DemandResponse setCompletion(Long eventId, Long demandId, Long userId, CompletionRequest req) {
        Event event = eventService.getEntity(eventId);
        eventAccess.requireInternalView(event);
        Demand demand = getDemand(eventId, demandId);
        Long me = CurrentUser.get().id();
        if (!userId.equals(me) && !eventAccess.permissionsOn(event).contains(Permission.EVENT_EDIT)) {
            throw ApiException.forbidden("Você só pode informar a conclusão das suas próprias demandas.");
        }
        DemandResponse current = single(eventId, demand);
        if (current.assignees().stream().noneMatch(a -> a.userId().equals(userId))) {
            throw ApiException.badRequest("Esta pessoa não é responsável por esta demanda.");
        }

        Optional<DemandCompletion> existing = completionRepository.findByDemandIdAndUserId(demandId, userId);
        if (req.completed() && existing.isEmpty()) {
            DemandCompletion c = new DemandCompletion();
            c.setDemand(demand);
            c.setUser(userRepository.getReferenceById(userId));
            c.setMarkedBy(userRepository.getReferenceById(me));
            completionRepository.saveAndFlush(c);
        } else if (!req.completed() && existing.isPresent()) {
            completionRepository.delete(existing.get());
            completionRepository.flush();
        }
        return single(eventId, demand);
    }

    private DemandResponse single(Long eventId, Demand demand) {
        return buildResponses(eventId, List.of(demand)).get(0);
    }

    private List<DemandResponse> buildResponses(Long eventId, List<Demand> demands) {
        Map<Long, List<DemandAssignment>> byDemand = assignmentRepository.findByEventId(eventId).stream()
                .collect(Collectors.groupingBy(a -> a.getDemand().getId()));
        Map<Long, Map<Long, DemandCompletion>> completions = completionRepository.findByEventId(eventId).stream()
                .collect(Collectors.groupingBy(c -> c.getDemand().getId(),
                        Collectors.toMap(c -> c.getUser().getId(), Function.identity())));
        List<EventTeamMember> team = teamRepository.findByEventIdOrderByUserNameAsc(eventId);
        Long me = CurrentUser.get().id();

        List<DemandResponse> result = new ArrayList<>();
        for (Demand d : demands) {
            List<DemandAssignment> assigned = byDemand.getOrDefault(d.getId(), List.of());
            List<FunctionAssignment> functions = assigned.stream()
                    .filter(a -> a.getFunction() != null)
                    .map(a -> new FunctionAssignment(a.getFunction().getId(), a.getFunction().getName(),
                            team.stream()
                                    .filter(m -> m.getFunction().getId().equals(a.getFunction().getId()))
                                    .map(m -> m.getUser().getName())
                                    .toList()))
                    .sorted(Comparator.comparing(FunctionAssignment::name))
                    .toList();
            List<UserAssignment> users = assigned.stream()
                    .filter(a -> a.getUser() != null)
                    .map(a -> new UserAssignment(a.getUser().getId(), a.getUser().getName()))
                    .sorted(Comparator.comparing(UserAssignment::name))
                    .toList();

            // Responsáveis efetivos: quem exerce uma função atribuída + quem foi atribuído diretamente.
            Map<Long, String> names = new LinkedHashMap<>();
            Map<Long, List<String>> via = new HashMap<>();
            Set<Long> functionIds = functions.stream().map(FunctionAssignment::id).collect(Collectors.toSet());
            for (EventTeamMember m : team) {
                if (functionIds.contains(m.getFunction().getId())) {
                    names.put(m.getUser().getId(), m.getUser().getName());
                    via.computeIfAbsent(m.getUser().getId(), k -> new ArrayList<>()).add(m.getFunction().getName());
                }
            }
            for (UserAssignment u : users) {
                names.putIfAbsent(u.id(), u.name());
                via.computeIfAbsent(u.id(), k -> new ArrayList<>()).add("Atribuição direta");
            }
            Map<Long, DemandCompletion> done = completions.getOrDefault(d.getId(), Map.of());
            List<Assignee> assignees = names.entrySet().stream()
                    .map(e -> {
                        DemandCompletion c = done.get(e.getKey());
                        return new Assignee(e.getKey(), e.getValue(), via.get(e.getKey()),
                                c != null, c == null ? null : c.getCompletedAt());
                    })
                    .sorted(Comparator.comparing(Assignee::name))
                    .toList();
            long completedCount = assignees.stream().filter(Assignee::completed).count();
            boolean mine = names.containsKey(me);
            boolean mineDone = mine && done.containsKey(me);

            result.add(new DemandResponse(d.getId(), d.getTitle(), d.getDescription(), d.getDueDate(),
                    functions, users, assignees, completedCount, mine, mineDone));
        }
        return result;
    }

    private Event editableEvent(Long eventId) {
        Event event = eventService.getEntity(eventId);
        eventAccess.require(event, Permission.EVENT_EDIT);
        return event;
    }

    private Demand getDemand(Long eventId, Long demandId) {
        Demand d = demandRepository.findById(demandId).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        CurrentUser.assertSameTenant(d.getTenantId(), NOT_FOUND);
        if (!d.getEvent().getId().equals(eventId)) {
            throw ApiException.notFound(NOT_FOUND);
        }
        return d;
    }

    private static void apply(Demand d, DemandRequest req) {
        d.setTitle(req.title().trim());
        d.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        d.setDueDate(req.dueDate());
    }
}
