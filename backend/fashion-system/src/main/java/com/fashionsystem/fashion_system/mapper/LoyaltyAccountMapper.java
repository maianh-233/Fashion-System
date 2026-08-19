package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.LoyaltyAccountDto;
import com.fashionsystem.fashion_system.entity.LoyaltyAccount;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link LoyaltyAccount} và {@link LoyaltyAccountDto}.
 */
@Component
public class LoyaltyAccountMapper {
    public LoyaltyAccountDto toDto(LoyaltyAccount entity) {
        if (entity == null) {
            return null;
        }
        LoyaltyAccountDto dto = new LoyaltyAccountDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public LoyaltyAccount toEntity(LoyaltyAccountDto dto) {
        if (dto == null) {
            return null;
        }
        LoyaltyAccount entity = new LoyaltyAccount();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

