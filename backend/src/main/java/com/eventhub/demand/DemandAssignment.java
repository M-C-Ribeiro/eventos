package com.eventhub.demand;

import com.eventhub.team.TeamFunction;
import com.eventhub.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

/**
 * Atribuição de uma demanda: OU a uma função (vale para todos que exercem essa função na equipe
 * do evento, inclusive quem entrar depois), OU a uma pessoa específica da equipe.
 */
@Entity
@Table(name = "atribuicoes_demanda")
@Getter
@Setter
@NoArgsConstructor
public class DemandAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demanda_id", nullable = false)
    private Demand demand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcao_id")
    private TeamFunction function;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User user;

    public static DemandAssignment ofFunction(Demand demand, TeamFunction function) {
        DemandAssignment a = new DemandAssignment();
        a.demand = demand;
        a.function = function;
        return a;
    }

    public static DemandAssignment ofUser(Demand demand, User user) {
        DemandAssignment a = new DemandAssignment();
        a.demand = demand;
        a.user = user;
        return a;
    }
}
