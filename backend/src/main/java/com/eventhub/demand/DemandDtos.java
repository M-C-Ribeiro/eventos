package com.eventhub.demand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class DemandDtos {

    private DemandDtos() {
    }

    public record DemandRequest(
            @NotBlank(message = "Informe o título") @Size(max = 160) String title,
            @Size(max = 2000, message = "A descrição pode ter até 2000 caracteres") String description,
            @NotNull(message = "Informe a data de entrega") LocalDate dueDate) {
    }

    /** Substitui as atribuições da demanda pelas funções e pessoas informadas (listas vazias = ninguém). */
    public record AssignRequest(@NotNull Set<Long> functionIds, @NotNull Set<Long> userIds) {
    }

    /** Função atribuída e quem a exerce hoje na equipe do evento. */
    public record FunctionAssignment(Long id, String name, List<String> members) {
    }

    public record UserAssignment(Long id, String name) {
    }

    /**
     * Um responsável efetivo pela demanda: atribuído diretamente e/ou por exercer uma função atribuída.
     * {@code via} lista as origens (nomes das funções e/ou "Atribuição direta").
     */
    public record Assignee(Long userId, String name, List<String> via, boolean completed, Instant completedAt) {
    }

    /**
     * {@code assignees}: todos os responsáveis com a situação de cada um.
     * {@code assignedToMe}/{@code completedByMe}: a situação do usuário logado, para "Minhas Demandas".
     */
    public record DemandResponse(
            Long id,
            String title,
            String description,
            LocalDate dueDate,
            List<FunctionAssignment> functions,
            List<UserAssignment> users,
            List<Assignee> assignees,
            long completedCount,
            boolean assignedToMe,
            boolean completedByMe) {
    }

    public record CompletionRequest(@NotNull(message = "Informe se a demanda foi concluída") Boolean completed) {
    }
}
