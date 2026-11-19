package com.eventhub.access;

import com.eventhub.access.AccessDtos.RoleRequest;
import com.eventhub.access.AccessDtos.RoleResponse;
import com.eventhub.common.ApiException;
import com.eventhub.security.CurrentUser;
import com.eventhub.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;

@Service
public class AccessRoleService {

    private static final String NOT_FOUND = "Perfil de acesso não encontrado.";

    private final AccessRoleRepository roleRepository;
    private final EntityManager entityManager;

    public AccessRoleService(AccessRoleRepository roleRepository, EntityManager entityManager) {
        this.roleRepository = roleRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAllByOrderBySystemRoleDescNameAsc().stream()
                .map(r -> RoleResponse.from(r, roleRepository.countUsers(r.getId())))
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest req) {
        String name = req.name().trim();
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("Já existe um perfil com este nome.");
        }
        AccessRole role = roleRepository.save(new AccessRole(name, blankToNull(req.description()), false, req.permissions()));
        return RoleResponse.from(role, 0);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest req) {
        AccessRole role = getEntity(id);
        if (role.isSystemRole()) {
            throw ApiException.badRequest("O perfil de administrador do sistema não pode ser alterado.");
        }
        String name = req.name().trim();
        if (roleRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict("Já existe um perfil com este nome.");
        }
        role.setName(name);
        role.setDescription(blankToNull(req.description()));
        role.setPermissions(new HashSet<>(req.permissions()));
        return RoleResponse.from(role, roleRepository.countUsers(id));
    }

    @Transactional
    public void delete(Long id) {
        AccessRole role = getEntity(id);
        if (role.isSystemRole()) {
            throw ApiException.badRequest("O perfil de administrador do sistema não pode ser excluído.");
        }
        // Remove o perfil de quem o possui antes de excluí-lo.
        List<User> holders = entityManager
                .createQuery("select u from User u join u.roles r where r.id = :id", User.class)
                .setParameter("id", id)
                .getResultList();
        holders.forEach(u -> u.getRoles().remove(role));
        entityManager.flush();
        roleRepository.delete(role);
    }

    private AccessRole getEntity(Long id) {
        AccessRole role = roleRepository.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        CurrentUser.assertSameTenant(role.getTenantId(), NOT_FOUND);
        return role;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
