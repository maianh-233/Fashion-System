package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.SupplierDto;
import com.fashionsystem.fashion_system.entity.Supplier;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Supplier} và {@link SupplierDto}.
 */
@Component
public class SupplierMapper {
    public SupplierDto toDto(Supplier entity) {
        if (entity == null) {
            return null;
        }
        SupplierDto dto = new SupplierDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Supplier toEntity(SupplierDto dto) {
        if (dto == null) {
            return null;
        }
        Supplier entity = new Supplier();
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(SupplierDto dto, Supplier entity) {
        entity.setCode(normalizeUpper(dto.getCode()));
        entity.setName(dto.getName().trim());
        entity.setContactName(trimToNull(dto.getContactName()));
        entity.setPhone(trimToNull(dto.getPhone()));
        entity.setEmail(normalizeEmail(dto.getEmail()));
        entity.setAddress(dto.getAddress());
        entity.setStatus(dto.getStatus() == null || dto.getStatus().isBlank()
                ? "ACTIVE" : normalizeUpper(dto.getStatus()));
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }

    private String normalizeUpper(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
