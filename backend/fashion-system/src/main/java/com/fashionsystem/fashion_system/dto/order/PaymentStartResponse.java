package com.fashionsystem.fashion_system.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Link thanh toán qua cổng cho khách.
 *
 * @param expiresAt hạn thanh toán của đơn; quá hạn đơn tự hủy
 */
public record PaymentStartResponse(UUID paymentId, String method, BigDecimal amount, String payUrl,
                                   LocalDateTime expiresAt) {
}
