package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerDto;
import com.fashionsystem.fashion_system.entity.Customer;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/** Chuyển đổi giữa Customer và DTO không lộ passwordHash. */
@Component
public class CustomerMapper {
    public CustomerDto toDto(Customer entity) {
        if (entity == null) return null;
        CustomerDto dto = new CustomerDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Customer toEntity(CustomerDto dto) {
        if (dto == null) return null;
        Customer entity = new Customer();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
