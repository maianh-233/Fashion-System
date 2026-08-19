package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.StoreStaffDto;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link StoreStaff} và {@link StoreStaffDto}.
 */
@Component
public class StoreStaffMapper {
    public StoreStaffDto toDto(StoreStaff entity) {
        if (entity == null) {
            return null;
        }
        StoreStaffDto dto = new StoreStaffDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public StoreStaff toEntity(StoreStaffDto dto) {
        if (dto == null) {
            return null;
        }
        StoreStaff entity = new StoreStaff();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

