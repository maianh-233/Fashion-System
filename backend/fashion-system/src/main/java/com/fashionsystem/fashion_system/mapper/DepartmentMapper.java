package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.DepartmentDto;
import com.fashionsystem.fashion_system.entity.Department;
import java.time.LocalDateTime;
import java.util.Locale;
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
        entity.setCreatedAt(LocalDateTime.now());
        entity.setActive(Boolean.TRUE);
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(DepartmentDto dto, Department entity) {
        entity.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription());
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
        if (entity.getId() != null) {
            entity.setUpdatedAt(LocalDateTime.now());
        }
    }
}
