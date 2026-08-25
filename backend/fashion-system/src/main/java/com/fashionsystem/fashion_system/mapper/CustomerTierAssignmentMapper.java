package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerTierAssignmentDto;
import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class CustomerTierAssignmentMapper {
    public CustomerTierAssignmentDto toDto(CustomerTierAssignment entity) {
        if (entity == null) return null;
        CustomerTierAssignmentDto dto = new CustomerTierAssignmentDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
    public CustomerTierAssignment toEntity(CustomerTierAssignmentDto dto) {
        if (dto == null) return null;
        CustomerTierAssignment entity = new CustomerTierAssignment();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
