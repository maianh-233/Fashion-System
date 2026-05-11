package com.fashion.orderservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.orderservice.entity.enums.ShippingStatus;

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
public class ShipmentResponse {

    private UUID id;

    private UUID orderId;

    private String shippingProvider;

    private String trackingCode;

    private ShippingStatus shippingStatus;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;
}