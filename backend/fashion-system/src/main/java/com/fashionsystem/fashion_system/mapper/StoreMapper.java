package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.entity.Store;
import java.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Store} và {@link StoreDto}.
 */
@Component
public class StoreMapper {
    public StoreDto toDto(Store entity) {
        if (entity == null) {
            return null;
        }
        StoreDto dto = new StoreDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Store toEntity(StoreDto dto) {
        if (dto == null) {
            return null;
        }
        Store entity = new Store();
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(StoreDto dto, Store entity) {
        entity.setName(dto.getName().trim());
        entity.setAddress(dto.getAddress());
        entity.setPhone(normalizePhone(dto.getPhone()));
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setActive(dto.getActive() == null ? Boolean.TRUE : dto.getActive());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }

    private String normalizePhone(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        normalized = normalized.replaceAll("[\\s().-]", "");
        return normalized.startsWith("+84") ? "0" + normalized.substring(3) : normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
