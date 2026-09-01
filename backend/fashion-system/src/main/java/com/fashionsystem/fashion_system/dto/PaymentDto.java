package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.DecimalMin;
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
 * DTO dùng để truyền dữ liệu của Payment giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private UUID id;
    @NotNull
    private UUID orderId;
    @Size(max = 50)
    private String paymentCode;
    @NotBlank @Size(max = 50)
    private String method;
    @NotNull @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal amount;
    private String status;
    private String transactionCode;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
