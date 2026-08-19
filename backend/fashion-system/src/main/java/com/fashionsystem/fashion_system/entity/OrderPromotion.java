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
 * Entity đại diện cho bảng {@code order_promotions}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "order_promotions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPromotion {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Lưu mã khuyến mãi của bản ghi. */
    @Column(name = "promotion_id", nullable = false)
    private UUID promotionId;

    /** Lưu giá trị promotion code của bản ghi. */
    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    /** Lưu giá trị discount amount của bản ghi. */
    @Column(name = "discount_amount", precision = 14, scale = 2)
    private BigDecimal discountAmount;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

