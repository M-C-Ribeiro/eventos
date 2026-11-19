package com.eventhub.meeting;

import com.eventhub.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Presença de um membro da equipe numa reunião. Quem não tem registro é considerado ausente.
 * A justificativa só existe para ausentes.
 */
@Entity
@Table(name = "presencas_reuniao",
        uniqueConstraints = @UniqueConstraint(name = "uk_attendance_meeting_user", columnNames = {"reuniao_id", "usuario_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MeetingAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reuniao_id", nullable = false)
    private PrepMeeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "presente", nullable = false)
    private boolean present;

    @Column(name = "justificativa", length = 500)
    private String justification;

    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt = Instant.now();
}
