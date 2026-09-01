package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của GoodsReceipt giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptDto {
    private UUID id;
    @NotBlank @Size(max = 50)
    private String receiptCode;
    private UUID supplierId;
    @NotNull
    private UUID storeId;
    private UUID receivedBy;
    private UUID approvedBy;
    private LocalDateTime receiptDate;
    @Size(max = 50)
    private String status;
    private String note;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
