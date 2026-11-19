package com.eventhub.security;

import com.eventhub.tenant.TenantContext;
import com.eventhub.user.User;
import com.eventhub.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o token JWT, define o tenant da requisição e autentica o usuário.
 * O usuário e seus perfis são recarregados do banco a cada requisição, então desativações
 * e mudanças de perfis ou permissões valem imediatamente.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    jwtService.parse(header.substring(7)).ifPresent(this::authenticate);
                } catch (RuntimeException e) {
                    // Falha ao carregar o usuário: registra a causa real em vez de um 500 sem detalhes.
                    log.error("Falha ao autenticar a requisição {} {}", request.getMethod(), request.getRequestURI(), e);
                    throw e;
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(JwtService.TokenClaims claims) {
        TenantContext.setTenantId(claims.tenantId());
        User user = userRepository.findWithRolesById(claims.userId())
                .filter(u -> claims.tenantId().equals(u.getTenantId()))
                .filter(User::isActive)
                .orElse(null);
        if (user == null) {
            TenantContext.clear();
            return;
        }
        // RBAC: as autoridades do Spring Security são as permissões vindas dos perfis do usuário.
        AuthUser principal = AuthUser.of(user);
        var authorities = principal.permissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.name()))
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
