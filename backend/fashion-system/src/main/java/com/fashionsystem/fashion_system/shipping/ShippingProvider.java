package com.fashionsystem.fashion_system.shipping;

import com.fashionsystem.fashion_system.shipping.ShippingModels.CreatedShipment;
import com.fashionsystem.fashion_system.shipping.ShippingModels.ProviderShipment;
import com.fashionsystem.fashion_system.shipping.ShippingModels.Quote;
import com.fashionsystem.fashion_system.shipping.ShippingModels.ShipmentRequest;
import com.fashionsystem.fashion_system.shipping.ShippingModels.WebhookEvent;
import java.util.Optional;

/**
 * Hợp đồng với một hãng vận chuyển. Nghiệp vụ đơn hàng chỉ phụ thuộc interface này; thêm hãng mới
 * (GHN, GHTK...) chỉ cần viết thêm một bean cài đặt và đặt {@code SHIPPING_PROVIDER} tương ứng.
 */
public interface ShippingProvider {
    /** Mã hãng lưu ở shipments.shipping_provider và dùng trong URL webhook, ví dụ LALAMOVE. */
    String code();

    Quote quote(ShipmentRequest request);

    /** Đặt chuyến theo báo giá vừa lấy. */
    CreatedShipment createShipment(ShipmentRequest request, Quote quote);

    /** Hủy chuyến bên hãng (Lalamove chỉ cho phép khi đang tìm tài xế hoặc vừa nhận dưới 5 phút). */
    void cancelShipment(String providerOrderId);

    ProviderShipment getStatus(String providerOrderId);

    /**
     * Xác minh chữ ký và đọc webhook. Trả về empty khi body rỗng (hãng thử kết nối);
     * ném {@link InvalidWebhookException} khi chữ ký hoặc khóa API không khớp.
     *
     * @param path path của URL webhook mà hãng gọi tới (một phần của chuỗi ký)
     */
    Optional<WebhookEvent> parseWebhook(String path, String rawBody);
}
