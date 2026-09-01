package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link ProductVariant} và {@link ProductVariantDto}.
 */
@Component
public class ProductVariantMapper {
    public ProductVariantDto toDto(ProductVariant entity) {
        if (entity == null) {
            return null;
        }
        ProductVariantDto dto = new ProductVariantDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public ProductVariant toEntity(ProductVariantDto dto) {
        if (dto == null) {
            return null;
        }
        ProductVariant entity = new ProductVariant();
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(ProductVariantDto dto, ProductVariant entity) {
        entity.setSku(dto.getSku().trim().toUpperCase(Locale.ROOT));
        entity.setColor(trimToNull(dto.getColor()));
        entity.setSize(trimToNull(dto.getSize()));
        entity.setPrice(dto.getPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setWeight(dto.getWeight());
        entity.setBarcode(trimToNull(dto.getBarcode()));
        entity.setActive(dto.getActive() == null ? Boolean.TRUE : dto.getActive());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
