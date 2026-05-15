package com.fashion.chatservice.mapper;




import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.chatservice.dto.common.OrderChatMessageDto;
import com.fashion.chatservice.dto.request.SendOrderChatMessageRequest;
import com.fashion.chatservice.dto.response.OrderChatMessageResponse;
import com.fashion.chatservice.entity.OrderChatMessage;

@Mapper(
        config = MapperConfig.class,
        uses = {
                ChatAttachmentMapper.class
        }
)
public interface OrderChatMessageMapper {

    @Mapping(target = "roomId", source = "room.id")
    OrderChatMessageDto toDto(OrderChatMessage entity);

    @Mapping(target = "roomId", source = "room.id")
    OrderChatMessageResponse toResponse(OrderChatMessage entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OrderChatMessage toEntity(SendOrderChatMessageRequest request);
}
