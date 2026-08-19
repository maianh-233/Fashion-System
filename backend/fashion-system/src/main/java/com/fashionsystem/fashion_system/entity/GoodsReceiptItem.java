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
 * Entity đại diện cho bảng {@code goods_receipt_items}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "goods_receipt_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptItem {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã tham chiếu đến receipt. */
    @Column(name = "receipt_id", nullable = false)
    private UUID receiptId;

    /** Lưu mã biến thể sản phẩm của bản ghi. */
    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    /** Lưu giá trị sku của bản ghi. */
    @Column(name = "sku", length = 100)
    private String sku;

    /** Lưu giá trị product name của bản ghi. */
    @Column(name = "product_name", length = 255)
    private String productName;

    /** Lưu giá trị cost price của bản ghi. */
    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice;

    /** Lưu số lượng của bản ghi. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Lưu thành tiền của bản ghi. */
    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

