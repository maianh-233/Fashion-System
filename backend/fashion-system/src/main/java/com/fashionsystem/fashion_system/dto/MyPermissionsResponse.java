package com.fashionsystem.fashion_system.dto;

import com.fashionsystem.fashion_system.entity.PermissionScope;
import java.util.List;

/** Cây permission hiệu lực dùng trực tiếp để render module, menu và button. */
public record MyPermissionsResponse(List<ModuleNode> modules) {
    public record ModuleNode(
            String code,
            String name,
            String icon,
            List<GroupNode> groups) {
    }

    public record GroupNode(
            String code,
            String name,
            List<PermissionNode> permissions) {
    }

    public record PermissionNode(String code, String name, PermissionScope scope) {
    }
}
