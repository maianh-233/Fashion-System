package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** One-query projection for active employees whose effective permission covers a receipt Store. */
public interface PermissionRecipientRepository extends Repository<User, UUID> {

    @Query(value = """
            WITH target_permission AS (
                SELECT p.id
                  FROM permissions p
                  JOIN permission_groups pg ON pg.id = p.group_id
                  JOIN modules m ON m.id = pg.module_id
                 WHERE p.code = :permissionCode
                   AND m.active = true
            ),
            role_scopes AS (
                SELECT ur.user_id,
                       MAX(CASE rp.scope
                           WHEN 'ALL' THEN 5 WHEN 'STORE' THEN 4 WHEN 'DEPARTMENT' THEN 3
                           WHEN 'TEAM' THEN 2 WHEN 'SELF' THEN 1 ELSE 0 END) AS scope_rank
                  FROM user_roles ur
                  JOIN role_permissions rp ON rp.role_id = ur.role_id
                  JOIN target_permission tp ON tp.id = rp.permission_id
                 GROUP BY ur.user_id
            ),
            user_overrides AS (
                SELECT up.user_id, up.effect,
                       CASE up.scope
                           WHEN 'ALL' THEN 5 WHEN 'STORE' THEN 4 WHEN 'DEPARTMENT' THEN 3
                           WHEN 'TEAM' THEN 2 WHEN 'SELF' THEN 1 ELSE 0 END AS scope_rank
                  FROM user_permissions up
                  JOIN target_permission tp ON tp.id = up.permission_id
            ),
            active_users AS (
                SELECT u.id AS user_id,
                       COUNT(DISTINCT ss.store_id) FILTER (WHERE ss.active = true) AS active_store_count
                  FROM users u
                  LEFT JOIN store_staffs ss ON ss.user_id = u.id
                 WHERE u.active = true
                   AND u.locked = false
                   AND u.deleted_at IS NULL
                 GROUP BY u.id
            ),
            effective_permissions AS (
                SELECT au.user_id, au.active_store_count,
                       CASE
                           WHEN uo.effect = 'DENY' THEN 0
                           WHEN uo.effect = 'ALLOW' THEN uo.scope_rank
                           ELSE COALESCE(rs.scope_rank, 0)
                       END AS effective_scope
                  FROM active_users au
                  LEFT JOIN role_scopes rs ON rs.user_id = au.user_id
                  LEFT JOIN user_overrides uo ON uo.user_id = au.user_id
            )
            SELECT DISTINCT ep.user_id
              FROM effective_permissions ep
             WHERE (CAST(:excludedUserId AS uuid) IS NULL OR ep.user_id <> :excludedUserId)
               AND (
                    (ep.active_store_count = 0 AND ep.effective_scope = 5)
                    OR
                    (ep.active_store_count = 1 AND ep.effective_scope >= 4 AND EXISTS (
                        SELECT 1
                          FROM store_staffs ss
                         WHERE ss.user_id = ep.user_id
                           AND ss.store_id = :storeId
                           AND ss.active = true
                    ))
               )
            """, nativeQuery = true)
    List<UUID> findEligibleUserIds(
            @Param("permissionCode") String permissionCode,
            @Param("storeId") UUID storeId,
            @Param("excludedUserId") UUID excludedUserId);
}
