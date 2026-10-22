package com.fashionsystem.fashion_system.shipping;

import com.fashionsystem.fashion_system.entity.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Dữ liệu trao đổi với hãng vận chuyển, không phụ thuộc hãng cụ thể. */
public final class ShippingModels {
    private ShippingModels() {
    }

    /** Điểm lấy/giao hàng. {@code phone} theo chuẩn E.164 (+84...). */
    public record Stop(String name, String phone, String address, double latitude, double longitude) {
    }

    /** Yêu cầu tạo chuyến cho một đơn của cửa hàng. */
    public record ShipmentRequest(String orderCode, Stop pickup, Stop dropoff, int itemQuantity, String remarks) {
    }

    /** Báo giá của hãng; Lalamove giữ giá 5 phút và cần stopId khi đặt chuyến. */
    public record Quote(String quotationId, BigDecimal fee, String currency, Instant expiresAt,
                        String pickupStopId, String dropoffStopId, String rawJson) {
    }

    /** Chuyến đã tạo bên hãng. */
    public record CreatedShipment(String providerOrderId, String providerStatus, ShipmentStatus status,
                                  String trackingUrl, BigDecimal fee, String currency, String rawJson) {
    }

    /** Trạng thái hiện tại của chuyến bên hãng (dùng khi cần đối soát). */
    public record ProviderShipment(String providerOrderId, String providerStatus, ShipmentStatus status,
                                   String trackingUrl, String rawJson) {
    }

    public enum WebhookType { STATUS_CHANGED, DRIVER_ASSIGNED, ORDER_REPLACED, OTHER }

    /**
     * Webhook đã xác minh chữ ký.
     *
     * @param status                 trạng thái nội bộ tương ứng (chỉ với STATUS_CHANGED)
     * @param previousProviderStatus trạng thái hãng trước đó (để biết hàng đã được lấy hay chưa)
     * @param previousProviderOrderId mã đơn cũ khi hãng thay thế chuyến (ORDER_REPLACED)
     * @param occurredAt             thời điểm cập nhật theo hãng, dùng để bỏ qua webhook đến muộn
     */
    public record WebhookEvent(String eventId, WebhookType type, String rawType, String providerOrderId,
                               String providerStatus, String previousProviderStatus, ShipmentStatus status,
                               String reason, String previousProviderOrderId, Driver driver, String trackingUrl,
                               Instant occurredAt, String rawJson) {
    }

    public record Driver(String name, String phone, String plateNumber) {
    }
}
