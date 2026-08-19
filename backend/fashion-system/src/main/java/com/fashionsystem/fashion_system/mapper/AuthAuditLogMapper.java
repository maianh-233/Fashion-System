package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.AuthAuditLogDto;
import com.fashionsystem.fashion_system.entity.AuthAuditLog;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link AuthAuditLog} và {@link AuthAuditLogDto}.
 */
@Component
public class AuthAuditLogMapper {
    public AuthAuditLogDto toDto(AuthAuditLog entity) {
        if (entity == null) {
            return null;
        }
        AuthAuditLogDto dto = new AuthAuditLogDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public AuthAuditLog toEntity(AuthAuditLogDto dto) {
        if (dto == null) {
            return null;
        }
        AuthAuditLog entity = new AuthAuditLog();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

