package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.NotificationLogDto;
import com.fashionsystem.fashion_system.entity.NotificationLog;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link NotificationLog} và {@link NotificationLogDto}.
 */
@Component
public class NotificationLogMapper {
    public NotificationLogDto toDto(NotificationLog entity) {
        if (entity == null) {
            return null;
        }
        NotificationLogDto dto = new NotificationLogDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public NotificationLog toEntity(NotificationLogDto dto) {
        if (dto == null) {
            return null;
        }
        NotificationLog entity = new NotificationLog();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

