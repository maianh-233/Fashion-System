package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PositionDto;
import com.fashionsystem.fashion_system.dto.position.CreatePositionRequest;
import com.fashionsystem.fashion_system.dto.position.UpdatePositionRequest;
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

    public Position toEntity(CreatePositionRequest request) {
        Position entity = new Position();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setActive(Boolean.TRUE);
        entity.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        entity.setDepartmentId(request.departmentId());
        entity.setName(request.name().trim());
        entity.setDescription(normalizeDescription(request.description()));
        if (request.active() != null) entity.setActive(request.active());
        entity.setHierarchyLevel(request.hierarchyLevel());
        entity.setMinSalary(request.minSalary());
        entity.setMaxSalary(request.maxSalary());
        return entity;
    }

    public void updateEntity(UpdatePositionRequest request, Position entity) {
        entity.setDepartmentId(request.departmentId());
        entity.setName(request.name().trim());
        entity.setDescription(normalizeDescription(request.description()));
        if (request.active() != null) entity.setActive(request.active());
        entity.setHierarchyLevel(request.hierarchyLevel());
        entity.setMinSalary(request.minSalary());
        entity.setMaxSalary(request.maxSalary());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
