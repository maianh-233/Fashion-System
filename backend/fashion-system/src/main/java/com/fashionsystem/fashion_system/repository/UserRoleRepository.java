package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.UserRole;
import com.fashionsystem.fashion_system.entity.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho UserRole.
 */
public interface UserRoleRepository extends BaseRepository<UserRole, UserRoleId> {

    /**
     * Lấy các liên kết vai trò của một người dùng.
     *
     * @param userId mã người dùng
     * @return danh sách vai trò đã gán
     */
    List<UserRole> findAllByUserId(UUID userId);
    boolean existsByRoleId(UUID roleId);

    default int deleteAllForUser(UUID userId) {
        var rows = findAllByUserId(userId);
        deleteAll(rows);
        flush();
        return rows.size();
    }

    default int deleteAssignment(UUID userId, UUID roleId) {
        var row = findById(new UserRoleId(userId, roleId));
        row.ifPresent(this::delete);
        flush();
        return row.isPresent() ? 1 : 0;
    }
}
