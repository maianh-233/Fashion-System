package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
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
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(CustomerTierDto dto, CustomerTier entity) {
        entity.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setName(dto.getName().trim());
        entity.setMinTotalSpent(dto.getMinTotalSpent() == null ? BigDecimal.ZERO : dto.getMinTotalSpent());
        entity.setDiscountPercent(dto.getDiscountPercent() == null ? BigDecimal.ZERO : dto.getDiscountPercent());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }
}
