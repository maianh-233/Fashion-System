package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.RolePermissionAuditDto;
import com.fashionsystem.fashion_system.entity.RolePermissionAudit;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link RolePermissionAudit} và {@link RolePermissionAuditDto}.
 */
@Component
public class RolePermissionAuditMapper {
    public RolePermissionAuditDto toDto(RolePermissionAudit entity) {
        if (entity == null) {
            return null;
        }
        RolePermissionAuditDto dto = new RolePermissionAuditDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public RolePermissionAudit toEntity(RolePermissionAuditDto dto) {
        if (dto == null) {
            return null;
        }
        RolePermissionAudit entity = new RolePermissionAudit();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

