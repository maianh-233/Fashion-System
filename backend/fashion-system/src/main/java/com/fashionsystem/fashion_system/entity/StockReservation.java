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
 * Entity đại diện cho bảng {@code stock_reservations}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Lưu mã cửa hàng của bản ghi. */
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /** Lưu mã biến thể sản phẩm của bản ghi. */
    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    /** Lưu số lượng của bản ghi. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", length = 30)
    private String status;

    /** Lưu giá trị expired at của bản ghi. */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

