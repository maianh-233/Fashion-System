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

/** Entity ánh xạ bảng {@code order_items}, lưu từng sản phẩm thuộc đơn hàng. */
@Entity
@Table(name = "order_items")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItem {
    /** Lưu mã định danh duy nhất của dòng đơn hàng. */
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "id", nullable = false)
    private UUID id;
    /** Lưu mã tham chiếu đến đơn hàng. */
    @Column(name = "order_id", nullable = false) private UUID orderId;
    /** Lưu mã tham chiếu đến sản phẩm. */
    @Column(name = "product_id") private UUID productId;
    /** Lưu mã tham chiếu đến biến thể sản phẩm. */
    @Column(name = "product_variant_id", nullable = false) private UUID productVariantId;
    /** Lưu tên sản phẩm tại thời điểm đặt hàng. */
    @Column(name = "product_name", nullable = false, length = 255) private String productName;
    /** Lưu mã SKU của sản phẩm. */
    @Column(name = "sku", length = 100) private String sku;
    /** Lưu màu sắc của sản phẩm. */
    @Column(name = "color", length = 100) private String color;
    /** Lưu kích thước của sản phẩm. */
    @Column(name = "size", length = 50) private String size;
    /** Lưu đường dẫn ảnh sản phẩm. */
    @Column(name = "image_url", columnDefinition = "TEXT") private String imageUrl;
    /** Lưu đơn giá tại thời điểm đặt hàng. */
    @Column(name = "price", nullable = false, precision = 12, scale = 2) private BigDecimal price;
    /** Lưu số lượng sản phẩm được đặt. */
    @Column(name = "quantity", nullable = false) private Integer quantity;
    /** Lưu thành tiền của dòng sản phẩm. */
    @Column(name = "total", nullable = false, precision = 14, scale = 2) private BigDecimal total;
    /** Lưu thời điểm tạo dòng đơn hàng. */
    @Column(name = "created_at") private LocalDateTime createdAt;
}
