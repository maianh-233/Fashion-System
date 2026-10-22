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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Một lần giao hàng của đơn qua bên vận chuyển; mỗi lần tạo chuyến là một dòng, không ghi đè. */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Lần tạo chuyến thứ mấy của đơn (1, 2, ...). */
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    /** Mã hãng vận chuyển, ví dụ LALAMOVE. */
    @Column(name = "shipping_provider", length = 100)
    private String shippingProvider;

    /** Mã đơn bên hãng (Lalamove orderId). */
    @Column(name = "provider_order_id", length = 64)
    private String providerOrderId;

    @Column(name = "quotation_id", length = 64)
    private String quotationId;

    /** Mã vận đơn hiển thị cho nhân viên/khách; với Lalamove trùng mã đơn bên hãng. */
    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "tracking_url", columnDefinition = "TEXT")
    private String trackingUrl;

    /** Phí ship hãng báo (chi phí của cửa hàng). */
    @Column(name = "fee", precision = 14, scale = 2)
    private BigDecimal fee;

    @Column(name = "currency", length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", length = 50, nullable = false)
    private ShipmentStatus status;

    /** Trạng thái gốc của hãng ở lần cập nhật gần nhất. */
    @Column(name = "provider_status", length = 40)
    private String providerStatus;

    /** Thời điểm cập nhật theo hãng; webhook cũ hơn giá trị này bị bỏ qua (webhook có thể đến sai thứ tự). */
    @Column(name = "provider_updated_at")
    private LocalDateTime providerUpdatedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "driver_phone", length = 30)
    private String driverPhone;

    @Column(name = "driver_plate", length = 30)
    private String driverPlate;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "replaced_by_shipment_id")
    private UUID replacedByShipmentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
