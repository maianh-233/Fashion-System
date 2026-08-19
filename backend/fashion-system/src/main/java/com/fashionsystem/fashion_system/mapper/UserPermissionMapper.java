package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.UserPermissionDto;
import com.fashionsystem.fashion_system.entity.UserPermission;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link UserPermission} và {@link UserPermissionDto}.
 */
@Component
public class UserPermissionMapper {
    public UserPermissionDto toDto(UserPermission entity) {
        if (entity == null) {
            return null;
        }
        UserPermissionDto dto = new UserPermissionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public UserPermission toEntity(UserPermissionDto dto) {
        if (dto == null) {
            return null;
        }
        UserPermission entity = new UserPermission();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

