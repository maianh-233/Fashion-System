package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Đơn của khách. Với đơn bị tách, khách thấy một đơn kèm trạng thái từng phần (mỗi cửa hàng một phần).
 * Danh sách đơn để trống {@code history}, {@code payments}, {@code promotions}; chi tiết đơn điền đủ.
 *
 * @param canCancel      khách còn tự hủy được (tất cả phần chờ xác nhận và chưa thanh toán)
 * @param paymentDueAt   hạn thanh toán qua cổng; quá hạn chưa trả thì đơn tự hủy
 * @param canPay         còn tạo được link thanh toán (đang chờ thanh toán và chưa quá hạn)
 * @param canSwitchToCod còn đổi được sang thanh toán khi nhận hàng
 * @param itemCount      tổng số lượng sản phẩm
 */
public record CustomerOrderResponse(
        UUID id, String orderCode, LocalDateTime createdAt, OrderStatus status, String statusLabel,
        BigDecimal subtotal, BigDecimal discountTotal, BigDecimal shippingFee, BigDecimal total,
        String paymentMethod, String paymentStatus, boolean split, List<Part> parts,
        OrderDetailResponse.Address shippingAddress, String cancelReason, boolean canCancel,
        LocalDateTime paymentDueAt, boolean canPay, boolean canSwitchToCod, int itemCount, String note,
        List<History> history, List<PaymentLine> payments, List<AppliedPromotion> promotions) {

    /**
     * @param deliveryStatus trạng thái chuyến giao gần nhất (null khi chưa giao cho vận chuyển)
     * @param trackingUrl    link theo dõi của hãng vận chuyển
     */
    public record Part(UUID id, String orderCode, String storeName, OrderStatus status, String statusLabel,
                       List<OrderDetailResponse.Item> items, String deliveryStatus, String trackingUrl) {
    }

    /** Một mốc trạng thái; {@code partCode} là mã phần đơn khi đơn bị tách. */
    public record History(OrderStatus status, String statusLabel, LocalDateTime at, String note, String partCode) {
    }

    public record PaymentLine(String method, BigDecimal amount, String status, LocalDateTime paidAt,
                              LocalDateTime createdAt, String failureReason) {
    }

    public record AppliedPromotion(String code, BigDecimal discount) {
    }
}
