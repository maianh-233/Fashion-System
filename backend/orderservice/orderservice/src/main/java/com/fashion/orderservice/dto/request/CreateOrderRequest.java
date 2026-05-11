package com.fashion.orderservice.dto.request;

import java.util.List;
import java.util.UUID;

import com.fashion.orderservice.dto.common.OrderAddressDto;
import com.fashion.orderservice.entity.enums.OrderType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateOrderRequest {

    @NotNull
    private UUID userId;

    private UUID storeId;

    @NotNull
    private OrderType orderType;

    @Valid
    @NotEmpty
    private List<CreateOrderItemRequest> items;

    @Valid
    @NotNull
    private OrderAddressDto address;

    private String note;

    private String promotionCode;
}
