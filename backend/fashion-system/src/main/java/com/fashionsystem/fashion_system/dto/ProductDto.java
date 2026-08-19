package com.fashionsystem.fashion_system.dto;

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
    private String name;
    private String slug;
    private String description;
    private String material;
    private String fit;
    private String gender;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String imageUrl;
}

