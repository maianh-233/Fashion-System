package com.fashionsystem.fashion_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của PaymentTransaction giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionDto {
    private UUID id;
    private UUID paymentId;
    private String gatewayTransactionId;
    private String transactionType;
    private BigDecimal amount;
    private String status;
    private String rawResponse;
    private LocalDateTime createdAt;
}

