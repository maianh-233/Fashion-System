package com.fashion.orderservice.dto.common;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.orderservice.entity.enums.OrderStatus;

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
public class OrderStatusHistoryDto {

    private OrderStatus fromStatus;

    private OrderStatus toStatus;

    private UUID changedBy;

    private String note;

    private LocalDateTime changedAt;
}
