package com.eventhub.access;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Perfil de acesso da organização: um nome e um conjunto de permissões. */
@Entity
@Table(name = "perfis_acesso",
        uniqueConstraints = @UniqueConstraint(name = "uk_access_roles_tenant_name", columnNames = {"organizacao_id", "nome"}))
@Getter
@Setter
@NoArgsConstructor
public class AccessRole {

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

    /** Perfil de administrador criado com a organização: não pode ser alterado nem excluído. */
    @Column(name = "perfil_sistema", nullable = false)
    private boolean systemRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_acesso_permissoes", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "permissao", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions = new HashSet<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AccessRole(String name, String description, boolean systemRole, Set<Permission> permissions) {
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
        this.permissions = permissions.isEmpty() ? new HashSet<>() : new HashSet<>(EnumSet.copyOf(permissions));
    }
}
