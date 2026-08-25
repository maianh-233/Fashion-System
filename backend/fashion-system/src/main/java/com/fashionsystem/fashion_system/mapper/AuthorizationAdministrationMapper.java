package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.*;
import com.fashionsystem.fashion_system.dto.RoleDto;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.repository.EffectivePermissionRow;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Mapping các aggregate chỉ dùng cho màn hình quản trị authorization. */
@Component
public class AuthorizationAdministrationMapper {
    public Catalog toCatalog(
            List<com.fashionsystem.fashion_system.entity.Module> modules,
            List<PermissionGroup> groups,
            List<Permission> permissions) {
        Map<UUID, List<Permission>> permissionsByGroup = permissions.stream()
                .collect(Collectors.groupingBy(Permission::getGroupId));
        Map<UUID, List<PermissionGroup>> groupsByModule = groups.stream()
                .collect(Collectors.groupingBy(PermissionGroup::getModuleId));

        List<CatalogModule> moduleNodes = modules.stream().map(module -> {
            List<CatalogGroup> groupNodes = groupsByModule.getOrDefault(module.getId(), List.of()).stream()
                    .map(group -> new CatalogGroup(group.getId(), group.getCode(), group.getName(),
                            permissionsByGroup.getOrDefault(group.getId(), List.of()).stream()
                                    .map(permission -> new CatalogPermission(
                                            permission.getId(), permission.getCode(), permission.getName()))
                                    .toList()))
                    .toList();
            return new CatalogModule(module.getId(), module.getCode(), module.getName(), module.getIcon(),
                    module.getSortOrder(), module.getActive(), groupNodes);
        }).toList();
        return new Catalog(moduleNodes);
    }

    public RoleDetails toRoleDetails(RoleDto role, List<EffectivePermissionRow> rows) {
        List<RolePermissionView> permissions = rows.stream()
                .map(row -> new RolePermissionView(
                        row.getPermissionId(), row.getPermissionCode(), row.getPermissionName(),
                        PermissionScope.valueOf(row.getScope()), row.getGroupCode(), row.getModuleCode()))
                .toList();
        return new RoleDetails(role, permissions);
    }
}
