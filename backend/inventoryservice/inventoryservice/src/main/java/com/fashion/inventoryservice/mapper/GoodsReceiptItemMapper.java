package com.fashion.inventoryservice.mapper;

import com.fashion.inventoryservice.dto.common.GoodsReceiptItemDto;
import com.fashion.inventoryservice.dto.request.goodsreceipt.CreateGoodsReceiptItemRequest;
import com.fashion.inventoryservice.entity.GoodsReceiptItem;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface GoodsReceiptItemMapper {

    GoodsReceiptItem toEntity(CreateGoodsReceiptItemRequest request);

    GoodsReceiptItemDto toDto(GoodsReceiptItem entity);

    default BigDecimal mapTotal(GoodsReceiptItem entity) {
        if (entity.getCostPrice() == null || entity.getQuantity() == null) {
            return BigDecimal.ZERO;
        }

        return entity.getCostPrice()
                .multiply(BigDecimal.valueOf(entity.getQuantity()));
    }
}