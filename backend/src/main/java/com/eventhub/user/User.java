package com.eventhub.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.eventhub.access.AccessRole;
import com.eventhub.access.Permission;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_tenant_email", columnNames = {"organizacao_id", "email"}))
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Preenchido e filtrado automaticamente pelo Hibernate a partir do TenantContext. */
    @TenantId
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "nome", nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String passwordHash;

    /** Perfis de acesso (RBAC). As permissões do usuário são a união das permissões dos perfis. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_perfis",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "perfil_id"))
    private Set<AccessRole> roles = new HashSet<>();

    @Column(name = "ativo", nullable = false)
    private boolean active = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** União das permissões de todos os perfis (os perfis precisam estar carregados). */
    public Set<Permission> effectivePermissions() {
        Set<Permission> result = EnumSet.noneOf(Permission.class);
        roles.forEach(r -> result.addAll(r.getPermissions()));
        return result;
    }

    public boolean hasSystemRole() {
        return roles.stream().anyMatch(AccessRole::isSystemRole);
    }
}
