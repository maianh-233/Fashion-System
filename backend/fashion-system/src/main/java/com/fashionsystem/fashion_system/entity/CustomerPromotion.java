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
 * Entity đại diện cho bảng {@code customer_promotions}: voucher riêng của khách, đổi bằng điểm tích lũy
 * ({@link #SOURCE_POINTS}) hoặc được tặng dịp sinh nhật ({@link #SOURCE_BIRTHDAY}, có thời hạn riêng).
 */
@Entity
@Table(name = "customer_promotions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPromotion {
    public static final String SOURCE_POINTS = "POINTS";
    public static final String SOURCE_BIRTHDAY = "BIRTHDAY";

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

    @Builder.Default
    @Column(name = "source", nullable = false, updatable = false)
    private String source = SOURCE_POINTS;

    /** Khoảng dùng được của voucher sinh nhật; null với voucher đổi điểm (theo thời gian của khuyến mãi). */
    @Column(name = "valid_from", updatable = false)
    private LocalDateTime validFrom;

    @Column(name = "expires_at", updatable = false)
    private LocalDateTime expiresAt;

    @Column(name = "birthday_year", updatable = false)
    private Integer birthdayYear;
}
