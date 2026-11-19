package com.eventhub.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Todas as consultas já são restritas ao tenant atual pelo Hibernate (@TenantId).
 * <p>
 * Os grafos usam o tipo LOAD e listam também "roles.permissions": com o tipo padrão (FETCH),
 * o Hibernate trata como LAZY tudo que não está no grafo, e as permissões não seriam
 * carregadas antes de a sessão fechar (o filtro JWT as lê fora de transação).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> findWithRolesById(Long id);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> findWithRolesByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"}, type = EntityGraph.EntityGraphType.LOAD)
    List<User> findAllByOrderByNameAsc();

    List<User> findByActiveTrueOrderByNameAsc();

    /** Usuários ativos com o perfil de administrador do sistema. */
    @Query("select count(distinct u) from User u join u.roles r where r.systemRole = true and u.active = true")
    long countActiveAdmins();
}
