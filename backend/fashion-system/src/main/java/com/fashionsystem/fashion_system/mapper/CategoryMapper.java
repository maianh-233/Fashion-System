package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CategoryDto;
import com.fashionsystem.fashion_system.entity.Category;
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
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

