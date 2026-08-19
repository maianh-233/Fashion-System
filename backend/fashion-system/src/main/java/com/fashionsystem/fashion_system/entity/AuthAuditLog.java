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
 * Entity đại diện cho bảng {@code auth_audit_logs}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "auth_audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthAuditLog {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã người dùng của bản ghi. */
    @Column(name = "user_id")
    private UUID userId;

    /** Lưu giá trị action của bản ghi. */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** Lưu mô tả chi tiết của bản ghi. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Lưu giá trị ip address của bản ghi. */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** Lưu giá trị user agent của bản ghi. */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

