package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.DepartmentDto;
import com.fashionsystem.fashion_system.entity.Department;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {
    public DepartmentDto toDto(Department entity) {
        if (entity == null) return null;
        DepartmentDto dto = new DepartmentDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
    public Department toEntity(DepartmentDto dto) {
        if (dto == null) return null;
        Department entity = new Department();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
