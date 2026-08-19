package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerAddressDto;
import com.fashionsystem.fashion_system.entity.CustomerAddress;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link CustomerAddress} và {@link CustomerAddressDto}.
 */
@Component
public class CustomerAddressMapper {
    public CustomerAddressDto toDto(CustomerAddress entity) {
        if (entity == null) {
            return null;
        }
        CustomerAddressDto dto = new CustomerAddressDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public CustomerAddress toEntity(CustomerAddressDto dto) {
        if (dto == null) {
            return null;
        }
        CustomerAddress entity = new CustomerAddress();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

