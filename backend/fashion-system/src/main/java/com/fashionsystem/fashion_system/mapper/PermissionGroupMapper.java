package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PermissionGroupDto;
import com.fashionsystem.fashion_system.entity.PermissionGroup;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Locale;

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
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(PermissionGroupDto dto, PermissionGroup entity) {
        entity.setModuleId(dto.getModuleId());
        entity.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription());
    }
}
