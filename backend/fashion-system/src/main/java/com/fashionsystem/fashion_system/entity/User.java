
package com.fashionsystem.fashion_system.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    /** Lưu tên đăng nhập duy nhất của người dùng. */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Mã nhân viên dùng trong nghiệp vụ HR/chấm công. */
    @Column(name = "employee_code", unique = true, length = 30)
    private String employeeCode;

    /** Họ tên đầy đủ của nhân viên. */
    @Column(name = "full_name", length = 255)
    private String fullName;

    /** Lưu địa chỉ email của bản ghi. */
    @Column(name = "email", unique = true, length = 255)
    private String email;

    /** Lưu số điện thoại của bản ghi. */
    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    /** Lưu mật khẩu đã băm của bản ghi. */
    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "avatar", columnDefinition = "TEXT")
    private String avatar;

    /** Chức danh nhân sự, độc lập với role phân quyền. */
    @Column(name = "job_title", length = 150)
    private String jobTitle;

    /** Vị trí chuẩn trong cơ cấu tổ chức; jobTitle được đồng bộ để giữ tương thích. */
    @Column(name = "position_id")
    private UUID positionId;

    @Column(name = "employment_type", length = 30)
    private String employmentType;

    @Column(name = "employment_status", length = 30)
    private String employmentStatus;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "work_location", length = 150)
    private String workLocation;

    /** Quản lý trực tiếp; tự tham chiếu đến một nhân viên khác. */
    @Column(name = "manager_id")
    private UUID managerId;

    /** Lưu trạng thái kích hoạt của bản ghi. */
    @Column(name = "active")
    private Boolean active;

    /** Lưu trạng thái khóa của bản ghi. */
    @Column(name = "locked")
    private Boolean locked;

    /** Lưu giá trị failed login attempts của bản ghi. */
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    /** Thời điểm kết thúc khóa tạm do đăng nhập sai nhiều lần; độc lập với khóa quản trị. */
    @Column(name = "login_locked_until")
    private LocalDateTime loginLockedUntil;

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
