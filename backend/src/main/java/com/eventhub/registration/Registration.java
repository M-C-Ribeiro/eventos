package com.eventhub.registration;

import com.eventhub.event.Event;
import com.eventhub.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/** Inscrição de um participante em um evento. O participante pode ou não ser usuário do sistema. */
@Entity
@Table(name = "inscricoes",
        uniqueConstraints = @UniqueConstraint(name = "uk_registrations_event_email", columnNames = {"evento_id", "email"}))
@Getter
@Setter
@NoArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Event event;

    /** Usuário do sistema vinculado à inscrição (inscrições antigas feitas pela própria conta). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User user;

    @Column(name = "nome", nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.CONFIRMED;

    @Column(name = "inscrito_em", nullable = false, updatable = false)
    private Instant registeredAt = Instant.now();

    @Column(name = "checkin_em")
    private Instant checkedInAt;
}
