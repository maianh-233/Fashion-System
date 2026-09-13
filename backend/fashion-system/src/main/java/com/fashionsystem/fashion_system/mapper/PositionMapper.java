package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PositionDto;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.entity.Position;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {
    public PositionDto toDto(Position entity, Department department) {
        if (entity == null) return null;
        return PositionDto.builder()
                .id(entity.getId()).departmentId(entity.getDepartmentId())
                .departmentCode(department == null ? null : department.getCode())
                .departmentName(department == null ? null : department.getName())
                .code(entity.getCode()).name(entity.getName()).description(entity.getDescription())
                .hierarchyLevel(entity.getHierarchyLevel()).minSalary(entity.getMinSalary())
                .maxSalary(entity.getMaxSalary())
                .active(entity.getActive()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Position toEntity(PositionDto dto) {
        Position entity = new Position();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setActive(Boolean.TRUE);
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(PositionDto dto, Position entity) {
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription() == null || dto.getDescription().isBlank()
                ? null : dto.getDescription().trim());
        if (dto.getActive() != null) entity.setActive(dto.getActive());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }
}
