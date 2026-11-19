package com.eventhub.auth;

import com.eventhub.auth.AuthDtos.*;
import com.eventhub.common.ApiException;
import com.eventhub.security.AuthUser;
import com.eventhub.security.JwtService;
import com.eventhub.tenant.Tenant;
import com.eventhub.tenant.TenantContext;
import com.eventhub.tenant.TenantRepository;
import com.eventhub.user.User;
import com.eventhub.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Os métodos aqui NÃO são @Transactional de propósito: o Hibernate define o tenant
 * de uma sessão quando ela é aberta, então cada chamada ao banco precisa abrir
 * sua própria sessão depois que o TenantContext correto foi definido.
 */
@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantBootstrap tenantBootstrap;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(TenantRepository tenantRepository, UserRepository userRepository,
                       TenantBootstrap tenantBootstrap, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.tenantBootstrap = tenantBootstrap;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        String slug = req.organizationSlug().trim().toLowerCase(Locale.ROOT);
        if (tenantRepository.existsBySlug(slug)) {
            throw ApiException.conflict("Já existe uma organização com o identificador \"" + slug + "\".");
        }
        Tenant tenant = tenantRepository.save(new Tenant(req.organizationName().trim(), slug));

        try {
            AuthUser admin = TenantContext.callAs(tenant.getId(), () -> tenantBootstrap.setUp(
                    req.name().trim(), normalizeEmail(req.email()), passwordEncoder.encode(req.password())));
            return buildResponse(admin, tenant);
        } catch (RuntimeException e) {
            tenantRepository.deleteById(tenant.getId());
            throw e;
        }
    }

    public AuthResponse login(LoginRequest req) {
        ApiException invalid = ApiException.unauthorized("Organização, e-mail ou senha inválidos.");
        Tenant tenant = tenantRepository.findBySlug(req.organizationSlug().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> invalid);

        User user = TenantContext.callAs(tenant.getId(),
                () -> userRepository.findWithRolesByEmailIgnoreCase(normalizeEmail(req.email())).orElse(null));

        if (user == null || !user.isActive() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw invalid;
        }
        return buildResponse(AuthUser.of(user), tenant);
    }

    public MeResponse me(AuthUser authUser) {
        Tenant tenant = tenantRepository.findById(authUser.tenantId())
                .orElseThrow(() -> ApiException.unauthorized("Organização não encontrada."));
        return toMe(authUser, tenant);
    }

    private AuthResponse buildResponse(AuthUser user, Tenant tenant) {
        String token = jwtService.generate(user.id(), tenant.getId());
        return new AuthResponse(token, jwtService.expirationSeconds(), toMe(user, tenant));
    }

    private static MeResponse toMe(AuthUser u, Tenant t) {
        return new MeResponse(u.id(), u.name(), u.email(), u.roles(), u.permissions(),
                new TenantInfo(t.getId(), t.getName(), t.getSlug()));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
