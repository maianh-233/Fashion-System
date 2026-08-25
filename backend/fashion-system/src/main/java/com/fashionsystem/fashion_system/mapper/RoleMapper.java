package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.RoleDto;
import com.fashionsystem.fashion_system.entity.Role;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Locale;

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
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(RoleDto dto, Role entity) {
        entity.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription());
    }
}
