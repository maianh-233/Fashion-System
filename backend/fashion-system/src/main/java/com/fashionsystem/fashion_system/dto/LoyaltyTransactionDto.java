package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của LoyaltyTransaction giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTransactionDto {
    private UUID id;
    private UUID userId;
    private String transactionType;
    private Integer points;
    private String referenceType;
    private UUID referenceId;
    private String note;
    private LocalDateTime createdAt;
}

