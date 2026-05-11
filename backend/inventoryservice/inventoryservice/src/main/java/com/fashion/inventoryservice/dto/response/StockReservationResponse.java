package com.fashion.inventoryservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationResponse {

    private UUID id;

    private UUID orderId;

    private UUID storeId;

    private UUID productVariantId;

    private Integer quantity;

    private String status;

    private LocalDateTime expiredAt;

    private LocalDateTime createdAt;
}