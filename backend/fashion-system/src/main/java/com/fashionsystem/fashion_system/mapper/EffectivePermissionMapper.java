package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.EffectivePermissionDto;
import com.fashionsystem.fashion_system.dto.MyPermissionsResponse;
import com.fashionsystem.fashion_system.dto.MyPermissionsResponse.GroupNode;
import com.fashionsystem.fashion_system.dto.MyPermissionsResponse.ModuleNode;
import com.fashionsystem.fashion_system.dto.MyPermissionsResponse.PermissionNode;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.repository.EffectivePermissionRow;
import java.util.*;
import org.springframework.stereotype.Component;

/** Mapping projection DB sang permission hiệu lực và response cây dành cho frontend. */
@Component
public class EffectivePermissionMapper {

    public EffectivePermissionDto toDto(EffectivePermissionRow row, String source) {
        return new EffectivePermissionDto(
                row.getPermissionId(), normalizeCode(row.getPermissionCode()), row.getPermissionName(),
                row.getGroupCode(), row.getGroupName(), row.getModuleCode(), row.getModuleName(),
                row.getModuleDescription(), row.getModuleIcon(), row.getModuleSortOrder(),
                PermissionScope.valueOf(row.getScope()), source);
    }

    public MyPermissionsResponse toTree(List<EffectivePermissionDto> permissions) {
        Map<String, List<EffectivePermissionDto>> byModule = permissions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        EffectivePermissionDto::moduleCode, LinkedHashMap::new, java.util.stream.Collectors.toList()));

        List<ModuleNode> modules = byModule.values().stream().map(modulePermissions -> {
            EffectivePermissionDto module = modulePermissions.getFirst();
            Map<String, List<EffectivePermissionDto>> byGroup = modulePermissions.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            EffectivePermissionDto::groupCode,
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()));
            List<GroupNode> groups = byGroup.values().stream().map(groupPermissions -> {
                EffectivePermissionDto group = groupPermissions.getFirst();
                List<PermissionNode> nodes = groupPermissions.stream()
                        .map(permission -> new PermissionNode(
                                permission.permissionCode(), permission.permissionName(), permission.scope()))
                        .toList();
                return new GroupNode(group.groupCode(), group.groupName(), nodes);
            }).toList();
            return new ModuleNode(module.moduleCode(), module.moduleName(), module.moduleIcon(), groups);
        }).toList();

        return new MyPermissionsResponse(modules);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
