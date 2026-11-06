package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval;

    @Column(name = "requires_documents", nullable = false)
    private Boolean requiresDocuments;

    @Column(name = "active", nullable = false)
    private Boolean active;

    /** Nghỉ có hưởng lương (tính vào ngày công được trả lương). */
    @Column(name = "paid", nullable = false)
    private Boolean paid;

    /** Số ngày tối đa mỗi năm; null = không giới hạn. */
    @Column(name = "annual_quota", precision = 5, scale = 1)
    private BigDecimal annualQuota;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
