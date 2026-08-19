package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PaymentTransactionDto;
import com.fashionsystem.fashion_system.entity.PaymentTransaction;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PaymentTransaction} và {@link PaymentTransactionDto}.
 */
@Component
public class PaymentTransactionMapper {
    public PaymentTransactionDto toDto(PaymentTransaction entity) {
        if (entity == null) {
            return null;
        }
        PaymentTransactionDto dto = new PaymentTransactionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PaymentTransaction toEntity(PaymentTransactionDto dto) {
        if (dto == null) {
            return null;
        }
        PaymentTransaction entity = new PaymentTransaction();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

