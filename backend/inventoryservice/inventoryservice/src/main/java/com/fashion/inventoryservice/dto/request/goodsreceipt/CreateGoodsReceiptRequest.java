package com.fashion.inventoryservice.dto.request.goodsreceipt;

import java.util.List;
import java.util.UUID;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGoodsReceiptRequest {

    private UUID supplierId;

    @NotNull
    private UUID storeId;

    private String note;

    @Valid
    @NotEmpty
    private List<CreateGoodsReceiptItemRequest> items;
}
