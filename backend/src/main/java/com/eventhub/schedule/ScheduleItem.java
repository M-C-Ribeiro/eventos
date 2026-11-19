package com.eventhub.schedule;

import com.eventhub.event.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Um item do cronograma de um evento (ex.: uma palestra). */
@Entity
@Table(name = "itens_cronograma")
@Getter
@Setter
@NoArgsConstructor
public class ScheduleItem {

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

    @Column(name = "data", nullable = false)
    private LocalDate date;

    @Column(name = "hora", nullable = false)
    private LocalTime time;

    @Column(name = "duracao_minutos", nullable = false)
    private int durationMinutes;

    @Column(name = "palestrante", nullable = false, length = 120)
    private String speaker;

    /** Telefone, e-mail ou outro meio de contato do palestrante. */
    @Column(name = "contato", length = 160)
    private String contact;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
