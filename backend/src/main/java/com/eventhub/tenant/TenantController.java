package com.eventhub.tenant;

import com.eventhub.auth.AuthDtos.TenantInfo;
import com.eventhub.common.ApiException;
import com.eventhub.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Dados da organização do usuário logado. */
@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final TenantRepository tenantRepository;

    public TenantController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public record UpdateTenantRequest(@NotBlank(message = "Informe o nome") @Size(max = 120) String name) {
    }

    @GetMapping
    public TenantInfo current() {
        Tenant t = load();
        return new TenantInfo(t.getId(), t.getName(), t.getSlug());
    }

    @PutMapping
    @Transactional
    @PreAuthorize("hasAuthority('ORGANIZATION_MANAGE')")
    public TenantInfo update(@Valid @RequestBody UpdateTenantRequest request) {
        Tenant t = load();
        t.setName(request.name().trim());
        return new TenantInfo(t.getId(), t.getName(), t.getSlug());
    }

    private Tenant load() {
        return tenantRepository.findById(CurrentUser.get().tenantId())
                .orElseThrow(() -> ApiException.notFound("Organização não encontrada."));
    }
}
