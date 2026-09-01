package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CollectionDto;
import com.fashionsystem.fashion_system.entity.Collection;
import java.time.LocalDateTime;
import java.util.Locale;
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
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(CollectionDto dto, Collection entity) {
        entity.setBrandId(dto.getBrandId());
        entity.setName(dto.getName().trim());
        entity.setCode(normalizeCode(dto.getCode()));
        entity.setSeason(trimToNull(dto.getSeason()));
        entity.setYear(dto.getYear());
        entity.setReleaseDate(dto.getReleaseDate());
        entity.setDescription(dto.getDescription());
        entity.setImageUrl(dto.getImageUrl());
        entity.setStatus(normalizeStatus(dto.getStatus()));
        if (entity.getId() != null) {
            entity.setUpdatedAt(LocalDateTime.now());
        }
    }

    private String normalizeCode(String code) {
        String normalized = trimToNull(code);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        return normalized == null ? "ACTIVE" : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
