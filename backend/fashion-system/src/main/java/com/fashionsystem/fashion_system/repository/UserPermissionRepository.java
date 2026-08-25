package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.UserPermission;
import com.fashionsystem.fashion_system.entity.UserPermissionId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho UserPermission.
 */
public interface UserPermissionRepository extends BaseRepository<UserPermission, UserPermissionId> {

    /**
     * Lấy các permission được cấp trực tiếp cho một user.
     *
     * @param userId mã user cần kiểm tra
     * @return các liên kết permission trực tiếp
     */
    List<UserPermission> findAllByUserId(UUID userId);
    boolean existsByPermissionId(UUID permissionId);

    @Modifying
    @Query("delete from UserPermission up where up.userId = :userId")
    int deleteAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from UserPermission up where up.userId = :userId and up.permissionId = :permissionId")
    int deleteOverride(@Param("userId") UUID userId, @Param("permissionId") UUID permissionId);

    /** Tải ALLOW/DENY trực tiếp của user trong một query. */
    @Query(value = """
            SELECT p.id AS permissionId, p.code AS permissionCode, p.name AS permissionName,
                   pg.code AS groupCode, pg.name AS groupName,
                   m.code AS moduleCode, m.name AS moduleName,
                   m.description AS moduleDescription, m.icon AS moduleIcon,
                   m.sort_order AS moduleSortOrder, up.scope AS scope,
                   up.effect AS effect
              FROM user_permissions up
              JOIN permissions p ON p.id = up.permission_id
              JOIN permission_groups pg ON pg.id = p.group_id
              JOIN modules m ON m.id = pg.module_id
             WHERE up.user_id = :userId AND m.active = true
            """, nativeQuery = true)
    List<EffectivePermissionRow> findOverridesByUserId(@Param("userId") UUID userId);
}
