
package com.fashion.inventoryservice.mapper;

import com.fashion.inventoryservice.dto.common.GoodsIssueItemDto;
import com.fashion.inventoryservice.dto.request.goodsissue.CreateGoodsIssueItemRequest;
import com.fashion.inventoryservice.entity.GoodsIssueItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GoodsIssueItemMapper {

    GoodsIssueItem toEntity(CreateGoodsIssueItemRequest request);

    GoodsIssueItemDto toDto(GoodsIssueItem entity);
}