package com.fashion.chatservice.mapper;


import org.mapstruct.Mapper;

import com.fashion.chatservice.dto.response.OrderIssueTypeResponse;
import com.fashion.chatservice.entity.OrderIssueType;

@Mapper(config = MapperConfig.class)
public interface OrderIssueTypeMapper {

    OrderIssueTypeResponse toResponse(OrderIssueType entity);
}
