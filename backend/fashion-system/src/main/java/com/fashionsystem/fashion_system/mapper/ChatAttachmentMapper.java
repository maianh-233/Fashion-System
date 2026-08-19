package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ChatAttachmentDto;
import com.fashionsystem.fashion_system.entity.ChatAttachment;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link ChatAttachment} và {@link ChatAttachmentDto}.
 */
@Component
public class ChatAttachmentMapper {
    public ChatAttachmentDto toDto(ChatAttachment entity) {
        if (entity == null) {
            return null;
        }
        ChatAttachmentDto dto = new ChatAttachmentDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public ChatAttachment toEntity(ChatAttachmentDto dto) {
        if (dto == null) {
            return null;
        }
        ChatAttachment entity = new ChatAttachment();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

