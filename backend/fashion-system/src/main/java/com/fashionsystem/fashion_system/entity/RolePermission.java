package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code role_permissions}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RolePermissionId.class)
public class RolePermission {
    /** Lưu mã vai trò của bản ghi. */
    @Id
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    /** Lưu mã quyền của bản ghi. */
    @Id
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

}

