package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerDto;
import com.fashionsystem.fashion_system.entity.Customer;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/** Chuyển đổi giữa Customer và DTO không lộ passwordHash. */
@Component
public class CustomerMapper {
    public CustomerDto toDto(Customer entity) {
        if (entity == null) return null;
        CustomerDto dto = new CustomerDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Customer toEntity(CustomerDto dto) {
        if (dto == null) return null;
        Customer entity = new Customer();
        entity.setActive(Boolean.TRUE);
        entity.setLocked(Boolean.FALSE);
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(CustomerDto dto, Customer entity) {
        entity.setUsername(dto.getUsername().trim().toLowerCase(Locale.ROOT));
        entity.setEmail(normalizeOptional(dto.getEmail(), true));
        entity.setPhone(normalizeOptional(dto.getPhone(), false));
        entity.setFullName(normalizeOptional(dto.getFullName(), false));
        entity.setDateOfBirth(dto.getDateOfBirth());
        String gender = normalizeOptional(dto.getGender(), false);
        entity.setGender(gender == null ? null : gender.toUpperCase(Locale.ROOT));
        entity.setAvatar(dto.getAvatar());
        if (dto.getActive() != null) entity.setActive(dto.getActive());
        if (dto.getLocked() != null) entity.setLocked(dto.getLocked());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }

    private String normalizeOptional(String value, boolean lowercase) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return lowercase ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }
}
