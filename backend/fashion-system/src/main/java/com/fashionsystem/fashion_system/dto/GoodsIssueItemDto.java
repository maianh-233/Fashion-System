package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của GoodsIssueItem giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsIssueItemDto {
    private UUID id;
    private UUID issueId;
    private UUID productVariantId;
    private String sku;
    private String productName;
    private Integer quantity;
    private LocalDateTime createdAt;
}

