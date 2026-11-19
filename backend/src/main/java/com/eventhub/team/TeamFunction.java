package com.eventhub.team;

import com.eventhub.access.Permission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Função que uma pessoa exerce na equipe de um evento (ex.: Coordenação, Recepção).
 * As permissões valem somente no evento em que a pessoa está na equipe.
 */
@Entity
@Table(name = "funcoes_equipe",
        uniqueConstraints = @UniqueConstraint(name = "uk_team_functions_tenant_name", columnNames = {"organizacao_id", "nome"}))
@Getter
@Setter
@NoArgsConstructor
public class TeamFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "nome", nullable = false, length = 60)
    private String name;

    @Column(name = "descricao", length = 255)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "funcao_equipe_permissoes", joinColumns = @JoinColumn(name = "funcao_id"))
    @Column(name = "permissao", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions = new HashSet<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TeamFunction(String name, String description, Set<Permission> permissions) {
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>(permissions);
    }
}
