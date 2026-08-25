package com.fashionsystem.fashion_system.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của User giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String username;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String avatar;
    private String jobTitle;
    private String employmentType;
    private String employmentStatus;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String workLocation;
    private UUID managerId;
    private Boolean active;
    private Boolean locked;
    private Integer failedLoginAttempts;
    private LocalDateTime lastPasswordChange;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
