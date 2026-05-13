package com.fashion.promotionservice.dto.request;

import com.fashion.promotionservice.dto.common.PromotionConditionDto;
import com.fashion.promotionservice.entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePromotionRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private DiscountType discountType;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal discountValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscount;

    private Integer usageLimit;

    private Integer usagePerUser;

    private Boolean active;

    private List<UUID> tierIds;

    private List<UUID> productIds;

    private List<UUID> categoryIds;

    private List<UUID> brandIds;

    private List<UUID> collectionIds;

    private List<PromotionConditionDto> conditions;
}