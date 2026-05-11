package com.fashion.orderservice.dto.common;

import java.time.LocalDateTime;

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
public class ShipmentDto {

    private String shippingProvider;

    private String trackingCode;

    private ShippingStatus shippingStatus;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;
}