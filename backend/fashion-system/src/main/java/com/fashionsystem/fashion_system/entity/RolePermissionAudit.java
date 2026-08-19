package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code role_permission_audit}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "role_permission_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionAudit {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã vai trò của bản ghi. */
    @Column(name = "role_id")
    private UUID roleId;

    /** Lưu mã quyền của bản ghi. */
    @Column(name = "permission_id")
    private UUID permissionId;

    /** Lưu giá trị action của bản ghi. */
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    /** Lưu giá trị changed by của bản ghi. */
    @Column(name = "changed_by")
    private UUID changedBy;

    /** Lưu giá trị changed at của bản ghi. */
    @Column(name = "changed_at")
    private LocalDateTime changedAt;

}

