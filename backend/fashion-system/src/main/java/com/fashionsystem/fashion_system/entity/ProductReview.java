package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Đánh giá của khách cho một sản phẩm trong một đơn đã hoàn thành (bảng {@code product_reviews});
 * mỗi cặp (đơn, sản phẩm) chỉ một đánh giá. Hiển thị ngay, nhân viên có thể ẩn và trả lời.
 */
@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Biến thể khách đã mua (để hiện màu/size); null nếu biến thể đã bị xóa. */
    @Column(name = "product_variant_id")
    private UUID productVariantId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** 1..5 sao. */
    @Column(name = "rating", nullable = false)
    private Short rating;

    @Column(name = "content", length = 2000)
    private String content;

    @Column(name = "hidden", nullable = false)
    private Boolean hidden;

    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;

    @Column(name = "hidden_by")
    private UUID hiddenBy;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    /** Phản hồi của cửa hàng, hiện dưới đánh giá. */
    @Column(name = "reply", length = 2000)
    private String reply;

    @Column(name = "replied_by")
    private UUID repliedBy;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
