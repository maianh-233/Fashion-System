package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.LoyaltyTransactionDto;
import com.fashionsystem.fashion_system.entity.LoyaltyTransaction;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link LoyaltyTransaction} và {@link LoyaltyTransactionDto}.
 */
@Component
public class LoyaltyTransactionMapper {
    public LoyaltyTransactionDto toDto(LoyaltyTransaction entity) {
        if (entity == null) {
            return null;
        }
        LoyaltyTransactionDto dto = new LoyaltyTransactionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public LoyaltyTransaction toEntity(LoyaltyTransactionDto dto) {
        if (dto == null) {
            return null;
        }
        LoyaltyTransaction entity = new LoyaltyTransaction();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

