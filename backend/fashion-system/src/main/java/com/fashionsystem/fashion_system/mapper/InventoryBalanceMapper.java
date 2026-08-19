package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.InventoryBalanceDto;
import com.fashionsystem.fashion_system.entity.InventoryBalance;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link InventoryBalance} và {@link InventoryBalanceDto}.
 */
@Component
public class InventoryBalanceMapper {
    public InventoryBalanceDto toDto(InventoryBalance entity) {
        if (entity == null) {
            return null;
        }
        InventoryBalanceDto dto = new InventoryBalanceDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public InventoryBalance toEntity(InventoryBalanceDto dto) {
        if (dto == null) {
            return null;
        }
        InventoryBalance entity = new InventoryBalance();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

