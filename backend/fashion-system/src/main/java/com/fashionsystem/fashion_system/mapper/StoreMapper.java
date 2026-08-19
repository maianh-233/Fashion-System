package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.entity.Store;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Store} và {@link StoreDto}.
 */
@Component
public class StoreMapper {
    public StoreDto toDto(Store entity) {
        if (entity == null) {
            return null;
        }
        StoreDto dto = new StoreDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Store toEntity(StoreDto dto) {
        if (dto == null) {
            return null;
        }
        Store entity = new Store();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

