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

/**
 * Entity đại diện cho bảng {@code loyalty_accounts}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "loyalty_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyAccount {
    /** Lưu mã người dùng của bản ghi. */
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lưu giá trị total spent của bản ghi. */
    @Column(name = "total_spent", precision = 14, scale = 2)
    private BigDecimal totalSpent;

    /** Lưu giá trị points balance của bản ghi. */
    @Column(name = "points_balance")
    private Integer pointsBalance;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

