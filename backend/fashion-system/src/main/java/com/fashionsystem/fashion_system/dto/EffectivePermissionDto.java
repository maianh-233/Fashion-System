package com.fashionsystem.fashion_system.dto;

import com.fashionsystem.fashion_system.entity.PermissionScope;
import java.util.UUID;

/** Permission cuối cùng sau khi hợp nhất role grants và user overrides. */
public record EffectivePermissionDto(
        UUID permissionId,
        String permissionCode,
        String permissionName,
        String groupCode,
        String groupName,
        String moduleCode,
        String moduleName,
        String moduleDescription,
        String moduleIcon,
        Integer moduleSortOrder,
        PermissionScope scope,
        String source) {
}
