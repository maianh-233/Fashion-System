package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Tích điểm khi đơn cửa hàng chuyển sang hoàn tất. Chạy đồng bộ trong cùng giao dịch đổi trạng thái để điểm và trạng
 * thái đơn luôn khớp nhau (giao dịch đổi trạng thái lỗi thì điểm cũng không được cộng).
 */
@Component
@RequiredArgsConstructor
public class LoyaltyOrderListener {
    private final LoyaltyEarningService loyalty;
    private final OrderRepository orders;

    @EventListener
    public void onStatusChanged(OrderEvents.StatusChanged event) {
        if (event.to() != OrderStatus.COMPLETED || event.customerId() == null || event.storeId() == null) return;
        orders.findById(event.orderId()).ifPresent(loyalty::earnForCompletedOrder);
    }
}
