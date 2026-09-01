package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.entity.Product;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Product} và {@link ProductDto}.
 */
@Component
public class ProductMapper {
    public ProductDto toDto(Product entity) {
        if (entity == null) {
            return null;
        }
        ProductDto dto = new ProductDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Product toEntity(ProductDto dto) {
        if (dto == null) {
            return null;
        }
        Product entity = new Product();
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(ProductDto dto, Product entity) {
        entity.setBrandId(dto.getBrandId());
        entity.setCollectionId(dto.getCollectionId());
        entity.setCategoryId(dto.getCategoryId());
        entity.setName(dto.getName().trim());
        entity.setSlug(normalizeSlug(dto.getSlug()));
        entity.setDescription(dto.getDescription());
        entity.setMaterial(trimToNull(dto.getMaterial()));
        entity.setFit(trimToNull(dto.getFit()));
        entity.setGender(normalizeUpper(dto.getGender()));
        entity.setStatus(dto.getStatus() == null || dto.getStatus().isBlank()
                ? "DRAFT" : normalizeUpper(dto.getStatus()));
        entity.setImageUrl(dto.getImageUrl().trim());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }

    private String normalizeSlug(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
