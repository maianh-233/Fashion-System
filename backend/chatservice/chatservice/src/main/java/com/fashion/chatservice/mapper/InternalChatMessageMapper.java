package com.fashion.chatservice.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.chatservice.dto.common.InternalChatMessageDto;
import com.fashion.chatservice.dto.request.SendInternalChatMessageRequest;
import com.fashion.chatservice.dto.response.InternalChatMessageResponse;
import com.fashion.chatservice.entity.InternalChatMessage;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

@Mapper(config = MapperConfig.class)
public interface InternalChatMessageMapper {

    @Mapping(target = "roomId", source = "room.id")
    InternalChatMessageDto toDto(InternalChatMessage entity);

    @Mapping(target = "roomId", source = "room.id")
    InternalChatMessageResponse toResponse(InternalChatMessage entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    InternalChatMessage toEntity(SendInternalChatMessageRequest request);
}