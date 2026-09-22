package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.entity.Brand;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Brand} và {@link BrandDto}.
 */
@Component
public class BrandMapper {
    public BrandDto toDto(Brand entity) {
        if (entity == null) {
            return null;
        }
        BrandDto dto = new BrandDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Brand toEntity(BrandDto dto) {
        if (dto == null) {
            return null;
        }
        Brand entity = new Brand();
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(BrandDto dto, Brand entity) {
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription());
        if (entity.getId() != null) {
            entity.setUpdatedAt(LocalDateTime.now());
        }
    }

    private String normalizeCode(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase(Locale.ROOT);
    }
}
