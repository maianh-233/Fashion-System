package com.fashion.orderservice.dto.common;

import java.math.BigDecimal;
import java.util.UUID;

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
public class OrderItemDto {

    private UUID productId;

    private UUID productVariantId;

    private String productName;

    private String sku;

    private String color;

    private String size;

    private String imageUrl;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal total;
}
