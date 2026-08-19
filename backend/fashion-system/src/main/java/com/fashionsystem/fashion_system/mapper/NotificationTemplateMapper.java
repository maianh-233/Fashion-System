package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.NotificationTemplateDto;
import com.fashionsystem.fashion_system.entity.NotificationTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link NotificationTemplate} và {@link NotificationTemplateDto}.
 */
@Component
public class NotificationTemplateMapper {
    public NotificationTemplateDto toDto(NotificationTemplate entity) {
        if (entity == null) {
            return null;
        }
        NotificationTemplateDto dto = new NotificationTemplateDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public NotificationTemplate toEntity(NotificationTemplateDto dto) {
        if (dto == null) {
            return null;
        }
        NotificationTemplate entity = new NotificationTemplate();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

