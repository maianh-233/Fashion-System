package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bảng trung gian nhiều-nhiều giữa nhân viên và phòng ban. */
@Entity
@Table(name = "user_departments")
@IdClass(UserDepartmentId.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserDepartment {
    @Id @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Id @Column(name = "department_id", nullable = false)
    private UUID departmentId;
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
}
