package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link CustomerTier} và {@link CustomerTierDto}.
 */
@Component
public class CustomerTierMapper {
    public CustomerTierDto toDto(CustomerTier entity) {
        if (entity == null) {
            return null;
        }
        CustomerTierDto dto = new CustomerTierDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public CustomerTier toEntity(CustomerTierDto dto) {
        if (dto == null) {
            return null;
        }
        CustomerTier entity = new CustomerTier();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

