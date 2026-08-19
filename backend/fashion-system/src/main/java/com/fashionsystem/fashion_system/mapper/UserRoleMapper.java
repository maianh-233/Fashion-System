package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.UserRoleDto;
import com.fashionsystem.fashion_system.entity.UserRole;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link UserRole} và {@link UserRoleDto}.
 */
@Component
public class UserRoleMapper {
    public UserRoleDto toDto(UserRole entity) {
        if (entity == null) {
            return null;
        }
        UserRoleDto dto = new UserRoleDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public UserRole toEntity(UserRoleDto dto) {
        if (dto == null) {
            return null;
        }
        UserRole entity = new UserRole();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

