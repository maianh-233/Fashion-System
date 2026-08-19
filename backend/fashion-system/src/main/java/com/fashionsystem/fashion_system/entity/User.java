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
 * Entity đại diện cho bảng {@code users}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu địa chỉ email của bản ghi. */
    @Column(name = "email", unique = true, length = 255)
    private String email;

    /** Lưu số điện thoại của bản ghi. */
    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    /** Lưu mật khẩu đã băm của bản ghi. */
    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    /** Lưu trạng thái kích hoạt của bản ghi. */
    @Column(name = "active")
    private Boolean active;

    /** Lưu trạng thái khóa của bản ghi. */
    @Column(name = "locked")
    private Boolean locked;

    /** Lưu giá trị failed login attempts của bản ghi. */
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    /** Lưu giá trị last password change của bản ghi. */
    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    /** Lưu giá trị email verified của bản ghi. */
    @Column(name = "email_verified")
    private Boolean emailVerified;

    /** Lưu giá trị phone verified của bản ghi. */
    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    /** Lưu giá trị last login của bản ghi. */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Lưu thời điểm xóa mềm của bản ghi. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}

