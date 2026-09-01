package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerTierAssignmentDto;
import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import java.time.LocalDateTime;
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
        entity.setAssignedAt(dto.getAssignedAt() == null ? LocalDateTime.now() : dto.getAssignedAt());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(CustomerTierAssignmentDto dto, CustomerTierAssignment entity) {
        entity.setCustomerId(dto.getCustomerId());
        entity.setTierId(dto.getTierId());
        if (dto.getAssignedAt() != null) entity.setAssignedAt(dto.getAssignedAt());
        entity.setExpiresAt(dto.getExpiresAt());
        entity.setNote(dto.getNote());
    }
}
