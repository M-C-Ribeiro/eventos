package com.eventhub.team;

import com.eventhub.event.Event;
import com.eventhub.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/** Vínculo de um usuário com a equipe de um evento, exercendo uma função. */
@Entity
@Table(name = "membros_equipe_evento",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_team_event_user", columnNames = {"evento_id", "usuario_id"}))
@Getter
@Setter
@NoArgsConstructor
public class EventTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcao_id", nullable = false)
    private TeamFunction function;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
