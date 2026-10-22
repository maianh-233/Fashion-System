package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.PromotionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Voucher hiển thị khi bán hàng.
 *
 * @param discount  số tiền giảm thực tế cho giỏ hiện tại (dùng để sắp xếp gợi ý)
 * @param eligible  dùng được ngay cho giỏ và khách hiện tại
 * @param reason    lý do chưa dùng được, ví dụ "Còn thiếu 150.000đ để đạt đơn tối thiểu"
 * @param suggested voucher giảm nhiều nhất trong nhóm cùng loại
 * @param selected  đang được chọn trong giỏ
 */
public record VoucherOptionResponse(
        UUID id, String code, String name, PromotionType type, String discountType, BigDecimal discountValue,
        BigDecimal maxDiscount, BigDecimal minOrderValue, LocalDateTime endDate,
        BigDecimal discount, boolean eligible, String reason, boolean suggested, boolean selected) {
}
