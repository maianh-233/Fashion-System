package com.fashionsystem.fashion_system.dto;

import com.fashionsystem.fashion_system.entity.PermissionScope;

public record PermissionCheckResponse(String permissionCode, PermissionScope requiredScope, boolean allowed) {
}
