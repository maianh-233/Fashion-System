package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.DecimalMin;
import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
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
 * DTO dùng để truyền dữ liệu của Order giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private UUID id;
    /** Do hệ thống sinh, bỏ qua giá trị client gửi lên. */
    @Size(max = 50)
    private String orderCode;
    private UUID customerId;
    private UUID storeId;
    @NotNull
    private OrderType orderType;
    private OrderStatus status;
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
    private UUID parentOrderId;
    private UUID handlerUserId;
    private UUID createdBy;
    private String paymentMethod;
    private BigDecimal cashReceived;
    private BigDecimal changeAmount;
    private String cancelReason;
    private OrderActorType cancelledByType;
    private Boolean active;
    private UUID hiddenBy;
    private LocalDateTime hiddenAt;
    private UUID restoredBy;
    private LocalDateTime restoredAt;
}
