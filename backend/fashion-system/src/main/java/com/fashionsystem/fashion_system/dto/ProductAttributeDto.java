package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của ProductAttribute giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeDto {
    private UUID id;
    private UUID productId;
    @NotBlank
    @Size(max = 100)
    private String attributeName;
    @NotBlank
    @Size(max = 255)
    private String attributeValue;
    private LocalDateTime createdAt;
}
