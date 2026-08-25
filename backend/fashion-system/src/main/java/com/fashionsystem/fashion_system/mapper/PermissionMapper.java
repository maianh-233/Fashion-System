package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PermissionDto;
import com.fashionsystem.fashion_system.entity.Permission;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Locale;

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
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(PermissionDto dto, Permission entity) {
        entity.setGroupId(dto.getGroupId());
        entity.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription());
    }
}
