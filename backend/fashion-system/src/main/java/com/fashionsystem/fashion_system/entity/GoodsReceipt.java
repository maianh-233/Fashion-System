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
 * Entity đại diện cho bảng {@code goods_receipts}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceipt {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu giá trị receipt code của bản ghi. */
    @Column(name = "receipt_code", nullable = false, unique = true, length = 50)
    private String receiptCode;

    /** Lưu mã tham chiếu đến supplier. */
    @Column(name = "supplier_id")
    private UUID supplierId;

    /** Lưu mã cửa hàng của bản ghi. */
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /** Lưu giá trị received by của bản ghi. */
    @Column(name = "received_by")
    private UUID receivedBy;

    /** Lưu giá trị approved by của bản ghi. */
    @Column(name = "approved_by")
    private UUID approvedBy;

    /** Lưu giá trị receipt date của bản ghi. */
    @Column(name = "receipt_date")
    private LocalDateTime receiptDate;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /** Lưu ghi chú của bản ghi. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Lưu giá trị total quantity của bản ghi. */
    @Column(name = "total_quantity")
    private Integer totalQuantity;

    /** Lưu giá trị total amount của bản ghi. */
    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

