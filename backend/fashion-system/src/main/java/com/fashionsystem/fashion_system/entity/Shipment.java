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
 * Entity đại diện cho bảng {@code shipments}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    /** Lưu giá trị shipping provider của bản ghi. */
    @Column(name = "shipping_provider", length = 100)
    private String shippingProvider;

    /** Lưu giá trị tracking code của bản ghi. */
    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    /** Lưu giá trị shipping status của bản ghi. */
    @Column(name = "shipping_status", length = 50)
    private String shippingStatus;

    /** Lưu giá trị shipped at của bản ghi. */
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    /** Lưu giá trị delivered at của bản ghi. */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

