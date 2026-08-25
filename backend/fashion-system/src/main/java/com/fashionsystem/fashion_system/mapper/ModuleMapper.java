package com.fashionsystem.fashion_system.mapper;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.fashionsystem.fashion_system.dto.ModuleDto;
import com.fashionsystem.fashion_system.entity.Module;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class ModuleMapper {
    public ModuleDto toDto(Module entity) {
        if (entity == null) return null;
        ModuleDto dto = new ModuleDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Module toEntity(ModuleDto dto) {
        Module entity = new Module();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setSortOrder(0);
        entity.setActive(Boolean.TRUE);
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(ModuleDto dto, Module entity) {
        entity.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription());
        entity.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
        if (dto.getActive() != null) entity.setActive(dto.getActive());
    }
}
