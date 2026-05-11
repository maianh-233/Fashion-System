package com.fashion.inventoryservice.mapper;

import com.fashion.inventoryservice.dto.request.goodsissue.CreateGoodsIssueRequest;
import com.fashion.inventoryservice.dto.response.GoodsIssueResponse;
import com.fashion.inventoryservice.entity.GoodsIssue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = {
                GoodsIssueItemMapper.class
        }
)
public interface GoodsIssueMapper {

    GoodsIssue toEntity(CreateGoodsIssueRequest request);

    @Mapping(target = "storeId", source = "store.id")
    GoodsIssueResponse toResponse(GoodsIssue entity);
}