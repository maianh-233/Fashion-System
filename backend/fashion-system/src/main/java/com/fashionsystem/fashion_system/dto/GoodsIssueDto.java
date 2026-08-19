package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của GoodsIssue giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsIssueDto {
    private UUID id;
    private String issueCode;
    private UUID storeId;
    private UUID orderId;
    private UUID issuedBy;
    private UUID approvedBy;
    private String issueType;
    private LocalDateTime issueDate;
    private String status;
    private String note;
    private Integer totalQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

