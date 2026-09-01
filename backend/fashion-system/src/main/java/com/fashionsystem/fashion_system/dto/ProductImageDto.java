package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của ProductImage giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
    private UUID id;
    private UUID productVariantId;
    @NotBlank
    private String imageUrl;
    private Boolean isPrimary;
    @Min(0)
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
