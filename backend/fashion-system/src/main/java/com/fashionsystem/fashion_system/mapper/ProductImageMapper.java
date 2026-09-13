package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductImageDto;
import com.fashionsystem.fashion_system.entity.ProductImage;
import java.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link ProductImage} và {@link ProductImageDto}.
 */
@Component
public class ProductImageMapper {
    public ProductImageDto toDto(ProductImage entity) {
        if (entity == null) {
            return null;
        }
        ProductImageDto dto = new ProductImageDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public ProductImage toEntity(ProductImageDto dto) {
        if (dto == null) {
            return null;
        }
        ProductImage entity = new ProductImage();
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(ProductImageDto dto, ProductImage entity) {
        if (dto.getImageUrl() != null) entity.setImageUrl(dto.getImageUrl().trim());
        if (dto.getCloudinaryPublicId() != null) {
            entity.setCloudinaryPublicId(dto.getCloudinaryPublicId().trim());
        }
        entity.setIsPrimary(dto.getIsPrimary() == null ? Boolean.FALSE : dto.getIsPrimary());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }
}
