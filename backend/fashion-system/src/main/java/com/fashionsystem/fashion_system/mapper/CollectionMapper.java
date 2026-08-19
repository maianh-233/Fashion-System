package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CollectionDto;
import com.fashionsystem.fashion_system.entity.Collection;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Collection} và {@link CollectionDto}.
 */
@Component
public class CollectionMapper {
    public CollectionDto toDto(Collection entity) {
        if (entity == null) {
            return null;
        }
        CollectionDto dto = new CollectionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Collection toEntity(CollectionDto dto) {
        if (dto == null) {
            return null;
        }
        Collection entity = new Collection();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

