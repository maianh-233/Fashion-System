package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PaymentDto;
import com.fashionsystem.fashion_system.entity.Payment;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Payment} và {@link PaymentDto}.
 */
@Component
public class PaymentMapper {
    public PaymentDto toDto(Payment entity) {
        if (entity == null) {
            return null;
        }
        PaymentDto dto = new PaymentDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Payment toEntity(PaymentDto dto) {
        if (dto == null) {
            return null;
        }
        Payment entity = new Payment();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

