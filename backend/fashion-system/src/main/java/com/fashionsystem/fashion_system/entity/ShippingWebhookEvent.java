package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Webhook đã nhận từ hãng vận chuyển; (provider, eventId) duy nhất để không xử lý trùng khi hãng gửi lại. */
@Entity
@Table(name = "shipping_webhook_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "provider_order_id", length = 64)
    private String providerOrderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    /** Kết quả xử lý: APPLIED, IGNORED_STALE, UNKNOWN_SHIPMENT, IGNORED_TYPE... */
    @Column(name = "result", nullable = false, length = 40)
    private String result;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
