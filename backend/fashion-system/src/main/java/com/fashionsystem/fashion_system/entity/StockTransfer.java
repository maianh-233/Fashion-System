package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phiếu điều chuyển tồn tại quầy giữa hai cửa hàng (bảng {@code stock_transfers}).
 * REQUESTED → SHIPPED → RECEIVED; hoặc REJECTED (nơi gửi) / CANCELLED (nơi yêu cầu, khi chưa duyệt).
 */
@Entity
@Table(name = "stock_transfers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "transfer_code", nullable = false, unique = true, length = 30)
    private String transferCode;

    /** Cửa hàng gửi (có hàng), duyệt và xuất. */
    @Column(name = "from_store_id", nullable = false)
    private UUID fromStoreId;

    /** Cửa hàng nhận (thiếu hàng), tạo yêu cầu và xác nhận đã nhận. */
    @Column(name = "to_store_id", nullable = false)
    private UUID toStoreId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "shipped_by")
    private UUID shippedBy;

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
