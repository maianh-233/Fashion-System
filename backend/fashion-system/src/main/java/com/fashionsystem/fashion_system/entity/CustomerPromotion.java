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
 * Entity đại diện cho bảng {@code customer_promotions}: khuyến mãi khách hàng đã đổi bằng điểm tích lũy.
 */
@Entity
@Table(name = "customer_promotions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPromotion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "promotion_id", nullable = false, updatable = false)
    private UUID promotionId;

    /** Số điểm thực tế đã trừ tại thời điểm đổi. */
    @Column(name = "points_spent", nullable = false, updatable = false)
    private Integer pointsSpent;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private LocalDateTime redeemedAt;

    /** Đơn đã dùng voucher đổi điểm này; null khi chưa dùng. Hủy/hoàn đơn không trả lại voucher. */
    @Column(name = "used_order_id")
    private UUID usedOrderId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
