package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code user_permissions}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "user_permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserPermissionId.class)
public class UserPermission {
    /** Lưu mã người dùng của bản ghi. */
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lưu mã quyền của bản ghi. */
    @Id
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

}

