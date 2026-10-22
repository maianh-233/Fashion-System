package com.fashionsystem.fashion_system.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Biến thể kèm tồn kho offline tại một cửa hàng, dùng cho màn hình bán tại quầy.
 *
 * @param unitPrice giá bán hiện tại: giá khuyến mãi niêm yết nếu có, ngược lại giá gốc
 * @param stock     tồn kho offline (available_quantity) tại cửa hàng
 */
public record PosVariantRow(
        UUID variantId, UUID productId, String productName, String productCode, String imageUrl,
        String sku, String barcode, String color, String size, BigDecimal price, BigDecimal salePrice,
        Boolean variantActive, String productStatus, Integer stock) {

    public BigDecimal unitPrice() {
        return salePrice != null ? salePrice : price;
    }

    /** Đang được bán: biến thể hoạt động và sản phẩm đang kinh doanh. */
    public boolean sellable() {
        return Boolean.TRUE.equals(variantActive) && "ACTIVE".equals(productStatus);
    }
}
