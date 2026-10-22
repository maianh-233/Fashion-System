package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.entity.ShipmentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết đơn cho trang quản trị.
 *
 * @param actions thao tác nhân viên hiện tại được làm với đơn (frontend chỉ hiện các nút này;
 *                backend vẫn kiểm tra lại khi thực hiện)
 * @param supportRoomId hội thoại hỗ trợ của khách về đơn này, null nếu khách chưa nhắn
 */
public record OrderDetailResponse(
        UUID id, String orderCode, OrderType orderType, OrderStatus status, String statusLabel,
        LocalDateTime createdAt, LocalDateTime updatedAt, String note,
        Ref store, Ref parentOrder, Customer customer, Address shippingAddress,
        List<Item> items, Totals totals, List<AppliedPromotion> promotions,
        PaymentInfo payment, List<ShipmentInfo> shipments,
        Person handler, Person createdBy, Cancellation cancellation, Visibility visibility,
        List<History> statusHistory, List<String> actions, UUID supportRoomId) {

    public record Ref(UUID id, String code, String name) {
    }

    /** Khách của đơn; null với khách vãng lai. */
    public record Customer(UUID id, String customerCode, String fullName, String phone, String email) {
    }

    /**
     * @param differentFromDefault khách chọn giao tới địa chỉ khác địa chỉ mặc định
     */
    public record Address(String receiverName, String receiverPhone, String addressLine, String ward,
                          String district, String province, boolean differentFromDefault) {
    }

    public record Item(UUID id, UUID productId, UUID variantId, String productName, String sku, String color,
                       String size, String imageUrl, int quantity, BigDecimal unitPrice, BigDecimal discount,
                       BigDecimal total, String promotionCode) {
    }

    public record Totals(BigDecimal subtotal, BigDecimal productDiscount, BigDecimal orderDiscount,
                         BigDecimal discountTotal, BigDecimal shippingFee, BigDecimal total,
                         BigDecimal cashReceived, BigDecimal changeAmount) {
    }

    public record AppliedPromotion(UUID promotionId, String code, String name, PromotionType type,
                                   BigDecimal discount) {
    }

    public record PaymentInfo(String method, String status, List<PaymentRecord> records, List<RefundInfo> refunds) {
    }

    public record PaymentRecord(UUID id, String method, BigDecimal amount, String status, String transactionCode,
                                LocalDateTime paidAt, LocalDateTime createdAt, List<GatewayTransaction> transactions) {
    }

    public record GatewayTransaction(String gatewayTransactionId, String type, BigDecimal amount, String status,
                                     LocalDateTime createdAt) {
    }

    /** Một lần tạo chuyến; danh sách sắp mới nhất trước. */
    public record ShipmentInfo(UUID id, int attemptNo, String provider, String trackingCode, String trackingUrl,
                               BigDecimal fee, String currency, ShipmentStatus status, String providerStatus,
                               String failureReason, String driverName, String driverPhone, String driverPlate,
                               LocalDateTime shippedAt, LocalDateTime deliveredAt, LocalDateTime createdAt) {
    }

    /** Yêu cầu hoàn tiền của đơn (đơn đã thanh toán qua cổng bị hủy). */
    public record RefundInfo(UUID id, BigDecimal amount, String status, String reason, LocalDateTime requestedAt) {
    }

    public record Person(UUID id, String name) {
    }

    public record Cancellation(String reason, OrderActorType cancelledBy) {
    }

    public record Visibility(boolean active, Person hiddenBy, LocalDateTime hiddenAt, Person restoredBy,
                             LocalDateTime restoredAt) {
    }

    public record History(OrderStatus fromStatus, OrderStatus toStatus, String toLabel, OrderActorType actorType,
                          String actorName, String reason, String note, LocalDateTime changedAt) {
    }
}
