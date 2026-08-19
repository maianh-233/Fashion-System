package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code payment_webhook_logs}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "payment_webhook_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookLog {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu giá trị provider của bản ghi. */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /** Lưu giá trị event type của bản ghi. */
    @Column(name = "event_type", length = 100)
    private String eventType;

    /** Lưu giá trị payload của bản ghi. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    /** Lưu giá trị processed của bản ghi. */
    @Column(name = "processed")
    private Boolean processed;

    /** Lưu giá trị processed at của bản ghi. */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

