package com.eventhub.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class AccessDtos {

    private AccessDtos() {
    }

    public record PermissionInfo(Permission code, String label, boolean eventScoped) {
        public static PermissionInfo from(Permission p) {
            return new PermissionInfo(p, p.label(), p.eventScoped());
        }
    }

    public record RoleSummary(Long id, String name) {
        public static RoleSummary from(AccessRole r) {
            return new RoleSummary(r.getId(), r.getName());
        }
    }

    public record RoleResponse(Long id, String name, String description, boolean systemRole,
                               List<Permission> permissions, long userCount) {
        public static RoleResponse from(AccessRole r, long userCount) {
            return new RoleResponse(r.getId(), r.getName(), r.getDescription(), r.isSystemRole(),
                    r.getPermissions().stream().sorted(Comparator.comparing(Enum::ordinal)).toList(), userCount);
        }
    }

    public record RoleRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 60) String name,
            @Size(max = 255) String description,
            @NotNull(message = "Informe as permissões") Set<Permission> permissions) {
    }
}
