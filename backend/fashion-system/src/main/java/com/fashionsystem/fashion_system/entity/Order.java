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
 * Entity đại diện cho bảng {@code orders}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu giá trị order code của bản ghi. */
    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    /** Lưu mã người dùng của bản ghi. */
    @Column(name = "user_id")
    private UUID userId;

    /** Lưu mã cửa hàng của bản ghi. */
    @Column(name = "store_id")
    private UUID storeId;

    /** Lưu giá trị order type của bản ghi. */
    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /** Lưu giá trị subtotal của bản ghi. */
    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal;

    /** Lưu giá trị discount total của bản ghi. */
    @Column(name = "discount_total", precision = 14, scale = 2)
    private BigDecimal discountTotal;

    /** Lưu giá trị tax của bản ghi. */
    @Column(name = "tax", precision = 14, scale = 2)
    private BigDecimal tax;

    /** Lưu giá trị shipping fee của bản ghi. */
    @Column(name = "shipping_fee", precision = 14, scale = 2)
    private BigDecimal shippingFee;

    /** Lưu giá trị total amount của bản ghi. */
    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** Lưu giá trị payment status của bản ghi. */
    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    /** Lưu ghi chú của bản ghi. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

