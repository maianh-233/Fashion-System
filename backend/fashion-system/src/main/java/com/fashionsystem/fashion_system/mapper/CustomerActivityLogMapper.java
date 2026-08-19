package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerActivityLogDto;
import com.fashionsystem.fashion_system.entity.CustomerActivityLog;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link CustomerActivityLog} và {@link CustomerActivityLogDto}.
 */
@Component
public class CustomerActivityLogMapper {
    public CustomerActivityLogDto toDto(CustomerActivityLog entity) {
        if (entity == null) {
            return null;
        }
        CustomerActivityLogDto dto = new CustomerActivityLogDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public CustomerActivityLog toEntity(CustomerActivityLogDto dto) {
        if (dto == null) {
            return null;
        }
        CustomerActivityLog entity = new CustomerActivityLog();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

