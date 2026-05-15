package com.fashion.chatservice.mapper;



import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.chatservice.dto.common.MessageStatusDto;
import com.fashion.chatservice.entity.OrderChatMessageStatus;

@Mapper(config = MapperConfig.class)
public interface OrderChatMessageStatusMapper {

    @Mapping(target = "messageId", source = "id.messageId")
    @Mapping(target = "userId", source = "id.userId")
    MessageStatusDto toDto(OrderChatMessageStatus entity);
}
