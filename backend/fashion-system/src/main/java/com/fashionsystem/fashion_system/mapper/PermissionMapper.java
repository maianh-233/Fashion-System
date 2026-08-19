package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PermissionDto;
import com.fashionsystem.fashion_system.entity.Permission;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Permission} và {@link PermissionDto}.
 */
@Component
public class PermissionMapper {
    public PermissionDto toDto(Permission entity) {
        if (entity == null) {
            return null;
        }
        PermissionDto dto = new PermissionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Permission toEntity(PermissionDto dto) {
        if (dto == null) {
            return null;
        }
        Permission entity = new Permission();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

