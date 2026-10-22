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

    /** Định danh sự kiện của cổng (mã giao dịch + kết quả); cổng gửi lại cùng sự kiện thì không xử lý lần hai. */
    @Column(name = "event_key", length = 200)
    private String eventKey;

    /** Lần thanh toán mà IPN này nói tới (null khi không tìm thấy hoặc sai chữ ký). */
    @Column(name = "payment_id")
    private UUID paymentId;

    /** Chữ ký của cổng có hợp lệ không. */
    @Column(name = "signature_valid")
    private Boolean signatureValid;

    /** Kết quả xử lý (CONFIRMED, ALREADY_PROCESSED, AMOUNT_MISMATCH...). */
    @Column(name = "result", length = 50)
    private String result;

}

