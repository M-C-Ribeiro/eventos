package com.eventhub.event;

import com.eventhub.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "titulo", nullable = false, length = 160)
    private String title;

    @Column(name = "descricao", length = 4000)
    private String description;

    @Column(name = "localizacao", length = 255)
    private String location;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "termino", nullable = false)
    private LocalDateTime endAt;

    /** Número máximo de inscrições confirmadas. Nulo = ilimitado. */
    @Column(name = "vagas")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private User organizer;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
