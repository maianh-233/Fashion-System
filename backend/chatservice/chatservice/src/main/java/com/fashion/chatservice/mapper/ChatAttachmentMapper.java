package com.fashion.chatservice.mapper;




import org.mapstruct.Mapper;

import com.fashion.chatservice.dto.common.ChatAttachmentDto;
import com.fashion.chatservice.dto.response.ChatAttachmentResponse;
import com.fashion.chatservice.entity.ChatAttachment;

@Mapper(config = MapperConfig.class)
public interface ChatAttachmentMapper {

    ChatAttachmentDto toDto(ChatAttachment entity);

    ChatAttachmentResponse toResponse(ChatAttachment entity);

    ChatAttachment toEntity(ChatAttachmentDto dto);
}