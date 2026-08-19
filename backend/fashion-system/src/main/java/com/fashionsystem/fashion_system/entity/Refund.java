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
 * Entity đại diện cho bảng {@code refunds}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "refunds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã tham chiếu đến payment. */
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    /** Lưu giá trị refund code của bản ghi. */
    @Column(name = "refund_code", unique = true, length = 50)
    private String refundCode;

    /** Lưu số tiền của bản ghi. */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Lưu giá trị reason của bản ghi. */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /** Lưu giá trị requested by của bản ghi. */
    @Column(name = "requested_by")
    private UUID requestedBy;

    /** Lưu giá trị requested at của bản ghi. */
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    /** Lưu giá trị processed at của bản ghi. */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

}

