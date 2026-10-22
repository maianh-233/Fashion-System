package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import java.util.List;
import java.util.UUID;

/**
 * Sự kiện nghiệp vụ đơn hàng, phát trong transaction qua {@code ApplicationEventPublisher}.
 * Listener gửi thông báo dùng {@code @TransactionalEventListener(AFTER_COMMIT)} nên chỉ chạy khi dữ liệu
 * đã lưu thành công.
 */
public final class OrderEvents {
    private OrderEvents() {
    }

    /** Mọi lần đổi trạng thái đơn của cửa hàng (phát duy nhất từ {@link OrderStatusService}). */
    public record StatusChanged(UUID orderId, String orderCode, OrderType orderType, UUID storeId, UUID customerId,
                                UUID parentOrderId, UUID handlerUserId, OrderStatus from, OrderStatus to,
                                OrderActor actor, String reason) {
    }

    /**
     * Khách vừa đặt đơn online. {@code split} = true khi đơn được tách cho nhiều cửa hàng; mỗi phần
     * liệt kê sản phẩm và cửa hàng giao để báo cho khách.
     */
    public record OnlineOrderPlaced(UUID orderId, String orderCode, UUID customerId, boolean split, List<Part> parts) {
        public record Part(UUID orderId, String orderCode, UUID storeId, String storeName, List<String> items) {
        }
    }

    /**
     * Chuyến giao không thành công theo báo cáo của hãng.
     *
     * @param goodsWithDriver hàng đã được lấy (đơn chuyển sang giao thất bại, chờ hoàn hàng); false = chưa lấy hàng,
     *                        đơn quay về "Đã xác nhận" để nhân viên liên hệ khách chờ hoặc hủy
     */
    public record ShipmentFailed(UUID orderId, String orderCode, UUID storeId, UUID handlerUserId, int attemptNo,
                                 String reason, boolean goodsWithDriver) {
    }

    /** Nhân viên gửi liên hệ cho khách; listener gửi email/thông báo và cập nhật trạng thái gửi. */
    public record CustomerContactRequested(UUID contactId) {
    }
}
