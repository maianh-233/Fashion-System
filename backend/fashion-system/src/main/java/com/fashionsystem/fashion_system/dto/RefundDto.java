package com.fashionsystem.fashion_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của Refund giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

