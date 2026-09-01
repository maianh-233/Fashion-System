package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerAddressDto;
import com.fashionsystem.fashion_system.entity.CustomerAddress;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link CustomerAddress} và {@link CustomerAddressDto}.
 */
@Component
public class CustomerAddressMapper {
    public CustomerAddressDto toDto(CustomerAddress entity) {
        if (entity == null) {
            return null;
        }
        CustomerAddressDto dto = new CustomerAddressDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public CustomerAddress toEntity(CustomerAddressDto dto) {
        if (dto == null) {
            return null;
        }
        CustomerAddress entity = new CustomerAddress();
        entity.setIsDefault(Boolean.FALSE);
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(CustomerAddressDto dto, CustomerAddress entity) {
        entity.setReceiverName(dto.getReceiverName().trim());
        entity.setReceiverPhone(dto.getReceiverPhone().trim());
        entity.setProvince(trimToNull(dto.getProvince()));
        entity.setDistrict(trimToNull(dto.getDistrict()));
        entity.setWard(trimToNull(dto.getWard()));
        entity.setAddressLine(dto.getAddressLine().trim());
        entity.setPostalCode(trimToNull(dto.getPostalCode()));
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setAddressType(normalizeType(dto.getAddressType()));
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }

    private String normalizeType(String value) {
        return value == null || value.isBlank() ? "HOME" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
