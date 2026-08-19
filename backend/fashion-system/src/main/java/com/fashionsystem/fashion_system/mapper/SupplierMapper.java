package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.SupplierDto;
import com.fashionsystem.fashion_system.entity.Supplier;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Supplier} và {@link SupplierDto}.
 */
@Component
public class SupplierMapper {
    public SupplierDto toDto(Supplier entity) {
        if (entity == null) {
            return null;
        }
        SupplierDto dto = new SupplierDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Supplier toEntity(SupplierDto dto) {
        if (dto == null) {
            return null;
        }
        Supplier entity = new Supplier();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

