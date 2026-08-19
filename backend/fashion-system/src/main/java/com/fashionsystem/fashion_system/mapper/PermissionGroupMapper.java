package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PermissionGroupDto;
import com.fashionsystem.fashion_system.entity.PermissionGroup;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PermissionGroup} và {@link PermissionGroupDto}.
 */
@Component
public class PermissionGroupMapper {
    public PermissionGroupDto toDto(PermissionGroup entity) {
        if (entity == null) {
            return null;
        }
        PermissionGroupDto dto = new PermissionGroupDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PermissionGroup toEntity(PermissionGroupDto dto) {
        if (dto == null) {
            return null;
        }
        PermissionGroup entity = new PermissionGroup();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

