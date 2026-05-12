package com.fashion.paymentservice.dto.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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