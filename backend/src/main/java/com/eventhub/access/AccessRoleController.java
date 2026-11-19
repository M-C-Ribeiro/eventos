package com.eventhub.access;

import com.eventhub.access.AccessDtos.PermissionInfo;
import com.eventhub.access.AccessDtos.RoleRequest;
import com.eventhub.access.AccessDtos.RoleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AccessRoleController {

    private final AccessRoleService roleService;

    public AccessRoleController(AccessRoleService roleService) {
        this.roleService = roleService;
    }

    /** Catálogo de permissões, com descrição e se podem ser dadas por função de equipe. */
    @GetMapping("/permissions")
    public List<PermissionInfo> permissions() {
        return Arrays.stream(Permission.values()).map(PermissionInfo::from).toList();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGE','USER_MANAGE')")
    public List<RoleResponse> list() {
        return roleService.list();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RoleResponse create(@Valid @RequestBody RoleRequest request) {
        return roleService.create(request);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return roleService.update(id, request);
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
