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
 * Entity đại diện cho bảng {@code product_variants}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã sản phẩm của bản ghi. */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Lưu giá trị sku của bản ghi. */
    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    /** Lưu giá trị color của bản ghi. */
    @Column(name = "color", length = 100)
    private String color;

    /** Lưu giá trị size của bản ghi. */
    @Column(name = "size", length = 50)
    private String size;

    /** Lưu đơn giá của bản ghi. */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Lưu giá trị sale price của bản ghi. */
    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    /** Lưu giá trị weight của bản ghi. */
    @Column(name = "weight", precision = 8, scale = 2)
    private BigDecimal weight;

    /** Lưu giá trị barcode của bản ghi. */
    @Column(name = "barcode", length = 100)
    private String barcode;

    /** Lưu trạng thái kích hoạt của bản ghi. */
    @Column(name = "active")
    private Boolean active;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

