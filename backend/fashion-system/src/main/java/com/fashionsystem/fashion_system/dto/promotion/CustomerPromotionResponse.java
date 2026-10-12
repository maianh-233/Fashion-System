package com.fashionsystem.fashion_system.dto.promotion;

import com.fashionsystem.fashion_system.dto.PromotionDto;
import java.time.LocalDateTime;
import java.util.UUID;

/** Khuyến mãi khách hàng đã đổi bằng điểm, kèm số dư điểm sau lần đổi (nếu có). */
public record CustomerPromotionResponse(
        UUID id,
        LocalDateTime redeemedAt,
        Integer pointsSpent,
        Integer pointsBalance,
        PromotionDto promotion) {
}
