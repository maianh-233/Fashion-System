package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.InternalChatMessageDto;
import com.fashionsystem.fashion_system.entity.InternalChatMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link InternalChatMessage} và {@link InternalChatMessageDto}.
 */
@Component
public class InternalChatMessageMapper {
    public InternalChatMessageDto toDto(InternalChatMessage entity) {
        if (entity == null) {
            return null;
        }
        InternalChatMessageDto dto = new InternalChatMessageDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public InternalChatMessage toEntity(InternalChatMessageDto dto) {
        if (dto == null) {
            return null;
        }
        InternalChatMessage entity = new InternalChatMessage();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

