package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.InventoryTransactionDto;
import com.fashionsystem.fashion_system.entity.InventoryTransaction;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link InventoryTransaction} và {@link InventoryTransactionDto}.
 */
@Component
public class InventoryTransactionMapper {
    public InventoryTransactionDto toDto(InventoryTransaction entity) {
        if (entity == null) {
            return null;
        }
        InventoryTransactionDto dto = new InventoryTransactionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public InventoryTransaction toEntity(InventoryTransactionDto dto) {
        if (dto == null) {
            return null;
        }
        InventoryTransaction entity = new InventoryTransaction();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

