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
 * Entity đại diện cho bảng {@code inventory_balances}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "inventory_balances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(InventoryBalanceId.class)
public class InventoryBalance {
    /** Lưu mã cửa hàng của bản ghi. */
    @Id
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /** Lưu mã biến thể sản phẩm của bản ghi. */
    @Id
    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    /** Lưu giá trị available quantity của bản ghi. */
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    /** Lưu giá trị reserved quantity của bản ghi. */
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    /** Lưu giá trị damaged quantity của bản ghi. */
    @Column(name = "damaged_quantity", nullable = false)
    private Integer damagedQuantity;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

