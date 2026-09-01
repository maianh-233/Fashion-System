package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotBlank @Size(max = 50)
    private String issueCode;
    @NotNull
    private UUID storeId;
    private UUID orderId;
    private UUID issuedBy;
    private UUID approvedBy;
    @NotBlank @Size(max = 50)
    private String issueType;
    private LocalDateTime issueDate;
    @Size(max = 50)
    private String status;
    private String note;
    private Integer totalQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
