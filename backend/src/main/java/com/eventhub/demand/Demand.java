package com.eventhub.demand;

import com.eventhub.event.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;

/** Uma tarefa a ser entregue para o evento, atribuída a funções e/ou pessoas da equipe. */
@Entity
@Table(name = "demandas")
@Getter
@Setter
@NoArgsConstructor
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Event event;

    @Column(name = "titulo", nullable = false, length = 160)
    private String title;

    @Column(name = "descricao", length = 2000)
    private String description;

    @Column(name = "data_entrega", nullable = false)
    private LocalDate dueDate;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
