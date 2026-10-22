package com.fashionsystem.fashion_system.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Đơn offline vừa tạo: mã đơn và tiền thối để nhân viên đưa khách. */
public record OfflineOrderCreatedResponse(
        UUID id, String orderCode, BigDecimal total, BigDecimal cashReceived, BigDecimal changeAmount,
        LocalDateTime createdAt) {
}
