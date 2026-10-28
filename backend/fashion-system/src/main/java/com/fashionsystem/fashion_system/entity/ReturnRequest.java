package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu đổi / trả hàng của khách cho một đơn đã hoàn thành (bảng {@code return_requests}).
 * REQUESTED → APPROVED → COMPLETED; hoặc REJECTED (cửa hàng) / CANCELLED (khách hủy khi chưa duyệt).
 */
@Entity
@Table(name = "return_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "request_code", nullable = false, unique = true, length = 30)
    private String requestCode;

    /** Đơn chứa hàng (đơn con nếu đơn online bị tách). */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** Cửa hàng đã bán, nơi khách mang hàng tới. */
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /** RETURN | EXCHANGE. */
    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reason_code", nullable = false, length = 30)
    private String reasonCode;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** Số tiền hoàn (chỉ trả hàng), chốt khi hoàn tất. */
    @Column(name = "refund_amount", precision = 14, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_id")
    private UUID refundId;

    @Column(name = "handled_by")
    private UUID handledBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
