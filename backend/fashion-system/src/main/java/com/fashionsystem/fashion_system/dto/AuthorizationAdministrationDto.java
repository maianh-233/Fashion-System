package com.fashionsystem.fashion_system.dto;

import com.fashionsystem.fashion_system.entity.PermissionEffect;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTO tổng hợp cho màn hình và thao tác quản trị authorization. */
public final class AuthorizationAdministrationDto {
    private AuthorizationAdministrationDto() {
    }

    public record RolePermissionGrant(@NotNull UUID permissionId, @NotNull PermissionScope scope) {
    }

    public record UserPermissionGrant(
            @NotNull UUID permissionId,
            @NotNull PermissionEffect effect,
            @NotNull PermissionScope scope) {
    }

    public record RoleIdsRequest(@NotNull Set<@NotNull UUID> roleIds) {
    }

    public record RoleDetails(RoleDto role, List<RolePermissionView> permissions) {
    }

    public record RolePermissionView(
            UUID permissionId, String code, String name, PermissionScope scope,
            String groupCode, String moduleCode) {
    }

    public record Catalog(List<CatalogModule> modules) {
    }

    public record CatalogModule(
            UUID id, String code, String name, String icon, Integer sortOrder,
            Boolean active, List<CatalogGroup> groups) {
    }

    public record CatalogGroup(UUID id, String code, String name, List<CatalogPermission> permissions) {
    }

    public record CatalogPermission(UUID id, String code, String name) {
    }
}
