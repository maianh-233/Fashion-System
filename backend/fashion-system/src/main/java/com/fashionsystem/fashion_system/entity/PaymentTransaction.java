package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
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
 * Entity đại diện cho bảng {@code payment_transactions}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã tham chiếu đến payment. */
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    /** Lưu mã tham chiếu đến gateway transaction. */
    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    /** Lưu giá trị transaction type của bản ghi. */
    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    /** Lưu số tiền của bản ghi. */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /** Lưu giá trị raw response của bản ghi. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response")
    private String rawResponse;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

