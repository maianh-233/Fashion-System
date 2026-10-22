package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Một dòng trong danh sách đơn quản trị.
 *
 * @param customerName  tên khách thành viên; null với khách vãng lai
 * @param receiverName  người nhận ở địa chỉ giao (đơn online)
 * @param itemCount     tổng số sản phẩm trong đơn
 */
public record OrderListRow(
        UUID id, String orderCode, OrderType orderType, OrderStatus status, LocalDateTime createdAt,
        UUID customerId, String customerName, String customerPhone, String receiverName, String receiverPhone,
        UUID handlerUserId, String handlerName, String paymentMethod, String paymentStatus, BigDecimal totalAmount,
        Long itemCount, Boolean active, UUID parentOrderId) {

    public String statusLabel() {
        return status == null ? null : status.label();
    }
}
