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

    /** ALLOW cấp trực tiếp, DENY thu hồi kể cả khi role đã cấp. */
    @Enumerated(EnumType.STRING)
    @Column(name = "effect", nullable = false, length = 10)
    @Builder.Default
    private PermissionEffect effect = PermissionEffect.ALLOW;

    /** Scope có ý nghĩa khi effect là ALLOW. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    @Builder.Default
    private PermissionScope scope = PermissionScope.ALL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", insertable = false, updatable = false)
    private Permission permission;

}
