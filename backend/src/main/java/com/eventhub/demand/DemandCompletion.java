package com.eventhub.demand;

import com.eventhub.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Conclusão de uma demanda por um dos responsáveis. Existir o registro = concluído;
 * voltar para pendente apaga o registro.
 */
@Entity
@Table(name = "conclusoes_demanda",
        uniqueConstraints = @UniqueConstraint(name = "uk_completion_demand_user", columnNames = {"demanda_id", "usuario_id"}))
@Getter
@Setter
@NoArgsConstructor
public class DemandCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demanda_id", nullable = false)
    private Demand demand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "concluido_em", nullable = false)
    private Instant completedAt = Instant.now();

    /** Quem registrou a conclusão (a própria pessoa ou a coordenação). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private User markedBy;
}
