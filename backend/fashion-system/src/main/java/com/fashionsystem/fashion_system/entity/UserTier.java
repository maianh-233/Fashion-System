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
 * Lịch sử gán hạng cho khách hàng, độc lập với tài khoản nhân viên.
 */
@Deprecated(forRemoval = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTier {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã khách hàng được gán hạng. */
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** Lưu mã hạng khách hàng của bản ghi. */
    @Column(name = "tier_id", nullable = false)
    private UUID tierId;

    /** Lưu thời điểm gán của bản ghi. */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    /** Lưu thời điểm hết hạn của bản ghi. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Lưu ghi chú của bản ghi. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

}
