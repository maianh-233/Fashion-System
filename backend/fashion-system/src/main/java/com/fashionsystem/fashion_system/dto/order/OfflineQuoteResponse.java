package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.PromotionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả tính tiền giỏ hàng tại quầy.
 *
 * @param warnings  vấn đề chặn tạo đơn (hết hàng, voucher không hợp lệ, thiếu tiền...)
 * @param notices   thông tin không chặn tạo đơn (voucher sản phẩm đã chọn nhưng không được áp dụng)
 * @param canCreate true khi không có cảnh báo và đã đủ tiền khách đưa
 */
public record OfflineQuoteResponse(
        UUID storeId,
        UUID customerId,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal productDiscount,
        BigDecimal orderDiscount,
        BigDecimal discountTotal,
        BigDecimal total,
        BigDecimal cashReceived,
        BigDecimal changeAmount,
        List<AppliedPromotion> promotions,
        List<String> warnings,
        List<String> notices,
        boolean canCreate) {

    /**
     * @param unitPrice giá bán một sản phẩm trước khuyến mãi sản phẩm
     * @param discount  giảm giá của khuyến mãi sản phẩm trên cả dòng
     * @param stock     tồn kho offline hiện tại tại cửa hàng
     */
    public record Line(
            UUID variantId, UUID productId, String productName, String sku, String color, String size,
            String imageUrl, BigDecimal unitPrice, int quantity, int stock, BigDecimal subtotal,
            BigDecimal discount, BigDecimal total, String promotionCode, String warning) {
    }

    public record AppliedPromotion(UUID id, String code, String name, PromotionType type, BigDecimal discount) {
    }
}
