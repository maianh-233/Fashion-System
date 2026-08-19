package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.UserTierDto;
import com.fashionsystem.fashion_system.entity.UserTier;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link UserTier} và {@link UserTierDto}.
 */
@Component
public class UserTierMapper {
    public UserTierDto toDto(UserTier entity) {
        if (entity == null) {
            return null;
        }
        UserTierDto dto = new UserTierDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public UserTier toEntity(UserTierDto dto) {
        if (dto == null) {
            return null;
        }
        UserTier entity = new UserTier();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

