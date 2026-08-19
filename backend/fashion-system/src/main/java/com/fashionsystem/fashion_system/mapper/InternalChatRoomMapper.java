package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.InternalChatRoomDto;
import com.fashionsystem.fashion_system.entity.InternalChatRoom;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link InternalChatRoom} và {@link InternalChatRoomDto}.
 */
@Component
public class InternalChatRoomMapper {
    public InternalChatRoomDto toDto(InternalChatRoom entity) {
        if (entity == null) {
            return null;
        }
        InternalChatRoomDto dto = new InternalChatRoomDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public InternalChatRoom toEntity(InternalChatRoomDto dto) {
        if (dto == null) {
            return null;
        }
        InternalChatRoom entity = new InternalChatRoom();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

