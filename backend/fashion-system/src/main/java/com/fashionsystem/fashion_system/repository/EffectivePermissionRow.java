package com.fashionsystem.fashion_system.repository;

import java.util.UUID;

/** Projection phẳng dùng để tải permission hiệu lực mà không hydrate cả cây entity. */
public interface EffectivePermissionRow {
    UUID getPermissionId();
    String getPermissionCode();
    String getPermissionName();
    String getGroupCode();
    String getGroupName();
    String getModuleCode();
    String getModuleName();
    String getModuleDescription();
    String getModuleIcon();
    Integer getModuleSortOrder();
    String getScope();
    String getEffect();
}
