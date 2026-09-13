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
 * DTO dùng để truyền dữ liệu của Product giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private UUID id;
    private UUID brandId;
    private UUID collectionId;
    private UUID categoryId;
    @NotBlank
    @Size(max = 255)
    private String name;
    @Size(max = 255)
    private String slug;
    private String description;
    @Size(max = 255)
    private String material;
    @Size(max = 100)
    private String fit;
    @Size(max = 20)
    private String gender;
    @Size(max = 50)
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String imageUrl;
}
