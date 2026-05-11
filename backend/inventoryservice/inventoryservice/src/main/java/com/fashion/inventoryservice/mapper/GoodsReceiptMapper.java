package com.fashion.inventoryservice.mapper;

import com.fashion.inventoryservice.dto.request.goodsreceipt.CreateGoodsReceiptRequest;
import com.fashion.inventoryservice.dto.response.GoodsReceiptResponse;
import com.fashion.inventoryservice.entity.GoodsReceipt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = {
                GoodsReceiptItemMapper.class
        }
)
public interface GoodsReceiptMapper {

    GoodsReceipt toEntity(CreateGoodsReceiptRequest request);

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "storeId", source = "store.id")
    GoodsReceiptResponse toResponse(GoodsReceipt entity);
}