package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.entity.Product;
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
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

