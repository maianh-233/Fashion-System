package com.fashionsystem.fashion_system.dto;

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
 * DTO dùng để truyền dữ liệu của Order giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private UUID id;
    @NotBlank @Size(max = 50)
    private String orderCode;
    private UUID customerId;
    private UUID storeId;
    @NotBlank @Size(max = 20)
    private String orderType;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    @DecimalMin("0.00")
    private BigDecimal tax;
    @DecimalMin("0.00")
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
