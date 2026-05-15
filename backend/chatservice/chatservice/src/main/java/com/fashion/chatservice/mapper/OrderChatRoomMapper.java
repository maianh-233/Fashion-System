package com.fashion.chatservice.mapper;




import org.mapstruct.Mapper;

import com.fashion.chatservice.dto.request.CreateOrderChatRoomRequest;
import com.fashion.chatservice.dto.response.OrderChatRoomResponse;
import com.fashion.chatservice.entity.OrderChatRoom;

@Mapper(
        config = MapperConfig.class,
        uses = {
                OrderChatMessageMapper.class
        }
)
public interface OrderChatRoomMapper {

    OrderChatRoomResponse toResponse(OrderChatRoom entity);

    OrderChatRoom toEntity(CreateOrderChatRoomRequest request);
}
    

