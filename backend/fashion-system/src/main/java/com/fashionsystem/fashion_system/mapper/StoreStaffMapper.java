package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.StoreStaffDto;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import java.time.LocalDateTime;
import java.util.Locale;
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
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(StoreStaffDto dto, StoreStaff entity) {
        entity.setUserId(dto.getUserId());
        entity.setStoreId(dto.getStoreId());
        entity.setStaffRole(dto.getStaffRole() == null || dto.getStaffRole().isBlank()
                ? null : dto.getStaffRole().trim().toUpperCase(Locale.ROOT));
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setActive(dto.getActive() == null ? Boolean.TRUE : dto.getActive());
    }
}
