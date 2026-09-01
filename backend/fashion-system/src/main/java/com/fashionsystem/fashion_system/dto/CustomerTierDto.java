package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của CustomerTier giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTierDto {
    private UUID id;
    @NotBlank @Size(max = 50)
    private String code;
    @NotBlank @Size(max = 100)
    private String name;
    @DecimalMin("0")
    private BigDecimal minTotalSpent;
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal discountPercent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
