package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.NotificationDto;
import com.fashionsystem.fashion_system.entity.Notification;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Notification} và {@link NotificationDto}.
 */
@Component
public class NotificationMapper {
    public NotificationDto toDto(Notification entity) {
        if (entity == null) {
            return null;
        }
        NotificationDto dto = new NotificationDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Notification toEntity(NotificationDto dto) {
        if (dto == null) {
            return null;
        }
        Notification entity = new Notification();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

