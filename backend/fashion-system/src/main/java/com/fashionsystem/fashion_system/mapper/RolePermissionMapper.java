package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.RolePermissionDto;
import com.fashionsystem.fashion_system.entity.RolePermission;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link RolePermission} và {@link RolePermissionDto}.
 */
@Component
public class RolePermissionMapper {
    public RolePermissionDto toDto(RolePermission entity) {
        if (entity == null) {
            return null;
        }
        RolePermissionDto dto = new RolePermissionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public RolePermission toEntity(RolePermissionDto dto) {
        if (dto == null) {
            return null;
        }
        RolePermission entity = new RolePermission();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

