package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CategoryDto;
import com.fashionsystem.fashion_system.entity.Category;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Category} và {@link CategoryDto}.
 */
@Component
public class CategoryMapper {
    public CategoryDto toDto(Category entity) {
        if (entity == null) {
            return null;
        }
        CategoryDto dto = new CategoryDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Category toEntity(CategoryDto dto) {
        if (dto == null) {
            return null;
        }
        Category entity = new Category();
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(CategoryDto dto, Category entity) {
        entity.setParentId(dto.getParentId());
        entity.setName(dto.getName().trim());
        entity.setCode(normalizeCode(dto.getCode()));
        entity.setImageUrl(dto.getImageUrl());
        if (entity.getId() != null) {
            entity.setUpdatedAt(LocalDateTime.now());
        }
    }

    private String normalizeCode(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
