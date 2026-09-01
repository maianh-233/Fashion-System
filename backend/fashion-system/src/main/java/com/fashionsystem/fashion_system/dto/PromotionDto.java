package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của Promotion giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDto {
    private UUID id;
    @NotBlank @Size(max = 50)
    private String code;
    @NotBlank @Size(max = 255)
    private String name;
    @NotBlank @Pattern(regexp = "PERCENT|FIXED")
    private String discountType;
    @NotNull @DecimalMin("0.00")
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @DecimalMin("0.00")
    private BigDecimal minOrderValue;
    @DecimalMin("0.00")
    private BigDecimal maxDiscount;
    @Min(1)
    private Integer usageLimit;
    @Min(1)
    private Integer usagePerUser;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
