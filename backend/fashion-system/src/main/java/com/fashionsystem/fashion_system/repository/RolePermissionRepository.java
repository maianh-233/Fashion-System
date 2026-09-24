package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.RolePermission;
import com.fashionsystem.fashion_system.entity.RolePermissionId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho RolePermission.
 */
public interface RolePermissionRepository extends BaseRepository<RolePermission, RolePermissionId> {

    /**
     * Lấy các liên kết permission của tập role để xây dựng authority.
     *
     * @param roleIds các mã role của user
     * @return các permission được cấp qua role
     */
    List<RolePermission> findAllByRoleIdIn(Collection<UUID> roleIds);
    List<RolePermission> findAllByRoleId(UUID roleId);
    boolean existsByRoleId(UUID roleId);
    boolean existsByPermissionId(UUID permissionId);

    default int deleteAllForRole(UUID roleId) {
        var rows = findAllByRoleId(roleId);
        deleteAll(rows);
        flush();
        return rows.size();
    }

    default int deleteGrant(UUID roleId, UUID permissionId) {
        var row = findById(new RolePermissionId(roleId, permissionId));
        row.ifPresent(this::delete);
        flush();
        return row.isPresent() ? 1 : 0;
    }

    @Query(value = """
            SELECT p.id AS permissionId, p.code AS permissionCode, p.name AS permissionName,
                   pg.code AS groupCode, pg.name AS groupName,
                   m.code AS moduleCode, m.name AS moduleName,
                   m.description AS moduleDescription, m.icon AS moduleIcon,
                   m.sort_order AS moduleSortOrder, rp.scope AS scope,
                   NULL::varchar AS effect
              FROM role_permissions rp
              JOIN permissions p ON p.id = rp.permission_id
              JOIN permission_groups pg ON pg.id = p.group_id
              JOIN modules m ON m.id = pg.module_id
             WHERE rp.role_id = :roleId
             ORDER BY m.sort_order, m.code, pg.code, p.code
            """, nativeQuery = true)
    List<EffectivePermissionRow> findPermissionDetailsByRoleId(@Param("roleId") UUID roleId);

    /** Tải toàn bộ role grants của user trong một query, kèm metadata cho API sidebar. */
    @Query(value = """
            SELECT p.id AS permissionId, p.code AS permissionCode, p.name AS permissionName,
                   pg.code AS groupCode, pg.name AS groupName,
                   m.code AS moduleCode, m.name AS moduleName,
                   m.description AS moduleDescription, m.icon AS moduleIcon,
                   m.sort_order AS moduleSortOrder, rp.scope AS scope,
                   NULL::varchar AS effect
              FROM user_roles ur
              JOIN role_permissions rp ON rp.role_id = ur.role_id
              JOIN permissions p ON p.id = rp.permission_id
              JOIN permission_groups pg ON pg.id = p.group_id
              JOIN modules m ON m.id = pg.module_id
             WHERE ur.user_id = :userId AND m.active = true
            """, nativeQuery = true)
    List<EffectivePermissionRow> findRoleGrantsByUserId(@Param("userId") UUID userId);
}
