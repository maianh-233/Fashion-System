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

    @Modifying
    @Query("delete from UserRole ur where ur.userId = :userId")
    int deleteAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from UserRole ur where ur.userId = :userId and ur.roleId = :roleId")
    int deleteAssignment(@Param("userId") UUID userId, @Param("roleId") UUID roleId);
}
