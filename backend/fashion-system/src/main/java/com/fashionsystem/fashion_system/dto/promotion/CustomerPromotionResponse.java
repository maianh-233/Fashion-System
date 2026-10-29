package com.fashionsystem.fashion_system.dto.promotion;

import com.fashionsystem.fashion_system.dto.PromotionDto;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Voucher riêng của khách (đổi bằng điểm hoặc quà sinh nhật), kèm số dư điểm sau lần đổi (nếu có), đơn hàng đã
 * dùng voucher (null khi chưa dùng) và hạn dùng riêng của voucher sinh nhật.
 */
public record CustomerPromotionResponse(
        UUID id,
        LocalDateTime redeemedAt,
        Integer pointsSpent,
        Integer pointsBalance,
        PromotionDto promotion,
        UUID usedOrderId,
        LocalDateTime usedAt,
        String source,
        LocalDateTime validFrom,
        LocalDateTime expiresAt) {
}
