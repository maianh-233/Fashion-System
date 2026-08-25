package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PermissionGroup;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PermissionGroup.
 */
public interface PermissionGroupRepository extends BaseRepository<PermissionGroup, UUID> {
    java.util.List<PermissionGroup> findAllByModuleId(java.util.UUID moduleId);
    java.util.List<PermissionGroup> findAllByOrderByCodeAsc();
    boolean existsByModuleId(java.util.UUID moduleId);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, java.util.UUID id);
}
