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
 * Entity đại diện cho bảng {@code user_roles}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "user_roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserRoleId.class)
public class UserRole {
    /** Lưu mã người dùng của bản ghi. */
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lưu mã vai trò của bản ghi. */
    @Id
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    /** Lưu thời điểm gán của bản ghi. */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

}

