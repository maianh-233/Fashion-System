package com.fashion.orderservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.orderservice.entity.enums.OrderStatus;
import com.fashion.orderservice.entity.enums.OrderType;
import com.fashion.orderservice.entity.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;

    private String orderCode;

    private UUID userId;

    private UUID storeId;

    private OrderType orderType;

    private OrderStatus status;

    private BigDecimal subtotal;

    private BigDecimal discountTotal;

    private BigDecimal tax;

    private BigDecimal shippingFee;

    private BigDecimal totalAmount;

    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;
}