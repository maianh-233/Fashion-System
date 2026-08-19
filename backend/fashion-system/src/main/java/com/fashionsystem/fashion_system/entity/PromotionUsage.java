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
 * Entity đại diện cho bảng {@code promotion_usages}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "promotion_usages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionUsage {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã khuyến mãi của bản ghi. */
    @Column(name = "promotion_id", nullable = false)
    private UUID promotionId;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Lưu mã người dùng của bản ghi. */
    @Column(name = "user_id")
    private UUID userId;

    /** Lưu giá trị used at của bản ghi. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

}

