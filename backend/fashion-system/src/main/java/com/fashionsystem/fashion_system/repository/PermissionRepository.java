package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Permission;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Permission.
 */
public interface PermissionRepository extends BaseRepository<Permission, UUID> {
    java.util.Optional<Permission> findByCode(String code);
    java.util.List<Permission> findAllByOrderByCodeAsc();
    boolean existsByGroupId(java.util.UUID groupId);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, java.util.UUID id);
}
