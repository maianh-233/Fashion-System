package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.UserNotificationPreferenceDto;
import com.fashionsystem.fashion_system.entity.UserNotificationPreference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link UserNotificationPreference} và {@link UserNotificationPreferenceDto}.
 */
@Component
public class UserNotificationPreferenceMapper {
    public UserNotificationPreferenceDto toDto(UserNotificationPreference entity) {
        if (entity == null) {
            return null;
        }
        UserNotificationPreferenceDto dto = new UserNotificationPreferenceDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public UserNotificationPreference toEntity(UserNotificationPreferenceDto dto) {
        if (dto == null) {
            return null;
        }
        UserNotificationPreference entity = new UserNotificationPreference();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

