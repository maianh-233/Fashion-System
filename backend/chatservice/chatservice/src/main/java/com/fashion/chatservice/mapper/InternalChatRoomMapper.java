package com.fashion.chatservice.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.chatservice.dto.request.CreateInternalChatRoomRequest;
import com.fashion.chatservice.dto.response.InternalChatRoomResponse;
import com.fashion.chatservice.entity.InternalChatRoom;

@Mapper(
        config = MapperConfig.class,
        uses = {
                InternalChatMessageMapper.class
        }
)
public interface InternalChatRoomMapper {

    InternalChatRoomResponse toResponse(InternalChatRoom entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "messages", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    InternalChatRoom toEntity(CreateInternalChatRoomRequest request);
}