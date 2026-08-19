package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerProfileDto;
import com.fashionsystem.fashion_system.entity.CustomerProfile;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link CustomerProfile} và {@link CustomerProfileDto}.
 */
@Component
public class CustomerProfileMapper {
    public CustomerProfileDto toDto(CustomerProfile entity) {
        if (entity == null) {
            return null;
        }
        CustomerProfileDto dto = new CustomerProfileDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public CustomerProfile toEntity(CustomerProfileDto dto) {
        if (dto == null) {
            return null;
        }
        CustomerProfile entity = new CustomerProfile();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

