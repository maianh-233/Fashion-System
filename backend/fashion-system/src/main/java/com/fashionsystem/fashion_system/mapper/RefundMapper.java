package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.RefundDto;
import com.fashionsystem.fashion_system.entity.Refund;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Refund} và {@link RefundDto}.
 */
@Component
public class RefundMapper {
    public RefundDto toDto(Refund entity) {
        if (entity == null) {
            return null;
        }
        RefundDto dto = new RefundDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Refund toEntity(RefundDto dto) {
        if (dto == null) {
            return null;
        }
        Refund entity = new Refund();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

