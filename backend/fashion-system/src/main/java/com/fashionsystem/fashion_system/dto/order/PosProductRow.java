package com.fashionsystem.fashion_system.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

/** Sản phẩm còn bán được tại cửa hàng: khoảng giá, tổng tồn offline và số biến thể còn hàng. */
public record PosProductRow(
        UUID productId, String name, String code, String imageUrl,
        BigDecimal minPrice, BigDecimal maxPrice, Long stock, Long variantCount) {
}
