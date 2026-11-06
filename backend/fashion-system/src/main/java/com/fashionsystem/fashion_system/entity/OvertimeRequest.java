package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Đơn tăng ca; chỉ số giờ đã duyệt mới được tính lương. */
@Entity
@Table(name = "overtime_requests")
@Getter
@Setter
public class OvertimeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal hours;

    @Column(name = "approved_hours", precision = 4, scale = 2)
    private BigDecimal approvedHours;

    /** WEEKDAY, WEEKEND, HOLIDAY — xác định từ ngày làm, quyết định hệ số tăng ca. */
    @Column(name = "overtime_type", nullable = false, length = 20)
    private String overtimeType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approver_note", columnDefinition = "TEXT")
    private String approverNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
