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
 * Entity đại diện cho bảng {@code payments}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Lưu giá trị payment code của bản ghi. */
    @Column(name = "payment_code", unique = true, length = 50)
    private String paymentCode;

    /** Lưu giá trị method của bản ghi. */
    @Column(name = "method", nullable = false, length = 50)
    private String method;

    /** Lưu số tiền của bản ghi. */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /** Lưu giá trị transaction code của bản ghi. */
    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    /** Lưu giá trị paid at của bản ghi. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

