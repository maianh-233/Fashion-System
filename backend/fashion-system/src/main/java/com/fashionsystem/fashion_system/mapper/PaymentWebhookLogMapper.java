package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PaymentWebhookLogDto;
import com.fashionsystem.fashion_system.entity.PaymentWebhookLog;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PaymentWebhookLog} và {@link PaymentWebhookLogDto}.
 */
@Component
public class PaymentWebhookLogMapper {
    public PaymentWebhookLogDto toDto(PaymentWebhookLog entity) {
        if (entity == null) {
            return null;
        }
        PaymentWebhookLogDto dto = new PaymentWebhookLogDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PaymentWebhookLog toEntity(PaymentWebhookLogDto dto) {
        if (dto == null) {
            return null;
        }
        PaymentWebhookLog entity = new PaymentWebhookLog();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

