package com.eventhub.meeting;

import com.eventhub.event.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Reunião preparatória da equipe de um evento. */
@Entity
@Table(name = "reunioes_preparatorias")
@Getter
@Setter
@NoArgsConstructor
public class PrepMeeting {

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

    @Column(name = "localizacao", nullable = false, length = 255)
    private String location;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
