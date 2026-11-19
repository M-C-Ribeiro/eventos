package com.eventhub.auth;

import com.eventhub.access.AccessRole;
import com.eventhub.access.AccessRoleRepository;
import com.eventhub.access.DefaultAccess;
import com.eventhub.security.AuthUser;
import com.eventhub.team.TeamFunction;
import com.eventhub.team.TeamFunctionRepository;
import com.eventhub.user.User;
import com.eventhub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Prepara uma organização recém-criada: perfis de acesso padrão, funções de equipe padrão
 * e o primeiro usuário (com o perfil de administrador do sistema).
 * <p>
 * Deve ser chamado já com o TenantContext da nova organização definido, para que a sessão
 * do Hibernate seja aberta no tenant certo.
 */
@Service
public class TenantBootstrap {

    private final AccessRoleRepository roleRepository;
    private final TeamFunctionRepository functionRepository;
    private final UserRepository userRepository;

    public TenantBootstrap(AccessRoleRepository roleRepository, TeamFunctionRepository functionRepository,
                           UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.functionRepository = functionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthUser setUp(String adminName, String adminEmail, String passwordHash) {
        AccessRole admin = roleRepository.save(new AccessRole(DefaultAccess.ADMIN.name(),
                DefaultAccess.ADMIN.description(), true, DefaultAccess.ADMIN.permissions()));
        DefaultAccess.ROLES.forEach(d ->
                roleRepository.save(new AccessRole(d.name(), d.description(), false, d.permissions())));
        DefaultAccess.TEAM_FUNCTIONS.forEach(d ->
                functionRepository.save(new TeamFunction(d.name(), d.description(), d.permissions())));

        User user = new User();
        user.setName(adminName);
        user.setEmail(adminEmail);
        user.setPasswordHash(passwordHash);
        user.setRoles(new HashSet<>(Set.of(admin)));
        return AuthUser.of(userRepository.save(user));
    }
}
