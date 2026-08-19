package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.RoleDto;
import com.fashionsystem.fashion_system.entity.Role;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Role} và {@link RoleDto}.
 */
@Component
public class RoleMapper {
    public RoleDto toDto(Role entity) {
        if (entity == null) {
            return null;
        }
        RoleDto dto = new RoleDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Role toEntity(RoleDto dto) {
        if (dto == null) {
            return null;
        }
        Role entity = new Role();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

