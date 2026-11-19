package com.eventhub.user;

import com.eventhub.access.AccessRole;
import com.eventhub.access.AccessRoleRepository;
import com.eventhub.common.ApiException;
import com.eventhub.demand.DemandAssignmentRepository;
import com.eventhub.demand.DemandCompletionRepository;
import com.eventhub.event.EventRepository;
import com.eventhub.meeting.MeetingAttendanceRepository;
import com.eventhub.registration.RegistrationRepository;
import com.eventhub.security.AuthUser;
import com.eventhub.security.CurrentUser;
import com.eventhub.team.EventTeamMemberRepository;
import com.eventhub.user.UserDtos.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserService {

    private static final String NOT_FOUND = "Usuário não encontrado.";
    private static final String NEED_ADMIN = "A organização precisa de pelo menos um usuário ativo com o perfil de administrador.";

    private final UserRepository userRepository;
    private final AccessRoleRepository roleRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EventTeamMemberRepository teamRepository;
    private final MeetingAttendanceRepository attendanceRepository;
    private final DemandAssignmentRepository demandAssignmentRepository;
    private final DemandCompletionRepository demandCompletionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, AccessRoleRepository roleRepository,
                       EventRepository eventRepository, RegistrationRepository registrationRepository,
                       EventTeamMemberRepository teamRepository, MeetingAttendanceRepository attendanceRepository,
                       DemandAssignmentRepository demandAssignmentRepository,
                       DemandCompletionRepository demandCompletionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.teamRepository = teamRepository;
        this.attendanceRepository = attendanceRepository;
        this.demandAssignmentRepository = demandAssignmentRepository;
        this.demandCompletionRepository = demandCompletionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAllByOrderByNameAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UserOption> activeOptions() {
        return userRepository.findByActiveTrueOrderByNameAsc().stream().map(UserOption::from).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Já existe um usuário com este e-mail na organização.");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setRoles(loadRoles(req.roleIds()));
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = getEntity(id);
        AuthUser me = CurrentUser.get();
        Set<AccessRole> newRoles = loadRoles(req.roleIds());

        boolean isAdminNow = user.isActive() && user.hasSystemRole();
        boolean staysAdmin = req.active() && newRoles.stream().anyMatch(AccessRole::isSystemRole);
        if (isAdminNow && !staysAdmin) {
            if (user.getId().equals(me.id())) {
                throw ApiException.badRequest("Você não pode remover seu próprio acesso de administrador.");
            }
            if (userRepository.countActiveAdmins() <= 1) {
                throw ApiException.badRequest(NEED_ADMIN);
            }
        }

        user.setName(req.name().trim());
        user.setRoles(newRoles);
        user.setActive(req.active());
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = getEntity(id);
        if (user.getId().equals(CurrentUser.get().id())) {
            throw ApiException.badRequest("Você não pode excluir a si mesmo.");
        }
        if (user.isActive() && user.hasSystemRole() && userRepository.countActiveAdmins() <= 1) {
            throw ApiException.badRequest(NEED_ADMIN);
        }
        if (eventRepository.existsByOrganizerId(user.getId())) {
            throw ApiException.conflict("Este usuário é responsável por eventos. Desative-o em vez de excluir.");
        }
        registrationRepository.detachUser(user.getId());
        teamRepository.deleteByUserId(user.getId());
        attendanceRepository.deleteByUserId(user.getId());
        demandAssignmentRepository.deleteByUserId(user.getId());
        demandCompletionRepository.deleteByUserId(user.getId());
        demandCompletionRepository.clearMarkedBy(user.getId());
        userRepository.delete(user);
    }

    private Set<AccessRole> loadRoles(Set<Long> ids) {
        List<AccessRole> roles = roleRepository.findAllByIdIn(ids);
        roles.forEach(r -> CurrentUser.assertSameTenant(r.getTenantId(), "Perfil de acesso não encontrado."));
        if (roles.size() != ids.size()) {
            throw ApiException.badRequest("Perfil de acesso não encontrado.");
        }
        return new HashSet<>(roles);
    }

    private User getEntity(Long id) {
        User user = userRepository.findWithRolesById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        CurrentUser.assertSameTenant(user.getTenantId(), NOT_FOUND);
        return user;
    }
}
