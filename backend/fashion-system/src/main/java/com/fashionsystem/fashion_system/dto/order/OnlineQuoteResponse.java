package com.fashionsystem.fashion_system.dto.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả tính tiền và phân cửa hàng cho giỏ online (chưa giữ hàng).
 *
 * @param parts     các phần đơn theo cửa hàng; nhiều phần nghĩa là đơn sẽ được tách
 * @param canPlace  không có cảnh báo chặn đặt hàng
 */
public record OnlineQuoteResponse(
        List<OfflineQuoteResponse.Line> lines, BigDecimal subtotal, BigDecimal productDiscount,
        BigDecimal orderDiscount, BigDecimal shippingFee, BigDecimal total,
        List<OfflineQuoteResponse.AppliedPromotion> promotions, List<Part> parts, boolean split,
        List<String> warnings, List<String> notices, boolean canPlace) {

    public record Part(UUID storeId, String storeName, String storeAddress, double distanceKm, List<PartItem> items) {
    }

    public record PartItem(UUID variantId, String productName, String sku, String color, String size, int quantity) {
    }
}
