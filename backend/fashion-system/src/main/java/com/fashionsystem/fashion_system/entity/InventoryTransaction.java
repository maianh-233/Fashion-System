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
 * Entity đại diện cho bảng {@code inventory_transactions}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã biến thể sản phẩm của bản ghi. */
    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    /** Lưu mã cửa hàng của bản ghi. */
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /** Lưu giá trị transaction type của bản ghi. */
    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType;

    /** Lưu giá trị reference type của bản ghi. */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /** Lưu mã tham chiếu đến reference. */
    @Column(name = "reference_id")
    private UUID referenceId;

    /** Lưu số lượng của bản ghi. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Lưu giá trị balance after của bản ghi. */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /** Lưu mã người tạo của bản ghi. */
    @Column(name = "created_by")
    private UUID createdBy;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

