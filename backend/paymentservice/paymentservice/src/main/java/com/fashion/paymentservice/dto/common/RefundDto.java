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
public class RefundDto {

    private UUID id;

    private UUID paymentId;

    private String refundCode;

    private BigDecimal amount;

    private String reason;

    private String status;

    private UUID requestedBy;

    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;
}