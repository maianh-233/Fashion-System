package com.fashionsystem.fashion_system.dto;

import com.fashionsystem.fashion_system.entity.PromotionStatus;
import com.fashionsystem.fashion_system.entity.PromotionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về thông tin khuyến mãi. Danh sách sản phẩm, hạng và số lượt dùng chỉ có ở API chi tiết.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDto {
    private UUID id;
    private String code;
    private String name;
    private PromotionType promotionType;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private Integer usageLimit;
    private Integer usagePerUser;
    private Integer pointsRequired;
    private PromotionStatus status;
    private LocalDateTime expiryRemindedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Sản phẩm được áp dụng (loại PRODUCT). */
    private List<Target> products;
    /** Hạng khách hàng được áp dụng; rỗng nghĩa là mọi khách hàng. */
    private List<Target> tiers;
    /** Số lượt đã dùng trên đơn hàng hoặc đã đổi bằng điểm. */
    private Long usedCount;
    /** Khi đã phát sinh lượt dùng thì không thể đổi kiểu giảm, giá trị giảm và ngày bắt đầu. */
    private Boolean inUse;

    /** Thông tin rút gọn của sản phẩm hoặc hạng khách hàng được áp dụng. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Target {
        private UUID id;
        private String code;
        private String name;
    }
}
