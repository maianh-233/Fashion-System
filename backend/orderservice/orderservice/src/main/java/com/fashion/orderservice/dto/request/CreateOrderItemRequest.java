package com.fashion.orderservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderItemRequest {

    private UUID productId;

    @NotNull
    private UUID productVariantId;

    @Min(1)
    private Integer quantity;
}
