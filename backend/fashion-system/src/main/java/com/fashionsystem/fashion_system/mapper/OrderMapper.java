package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderDto;
import com.fashionsystem.fashion_system.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Order} và {@link OrderDto}.
 */
@Component
public class OrderMapper {
    public OrderDto toDto(Order entity) {
        if (entity == null) {
            return null;
        }
        OrderDto dto = new OrderDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Order toEntity(OrderDto dto) {
        if (dto == null) {
            return null;
        }
        Order entity = new Order();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setStatus("PENDING");
        entity.setPaymentStatus("UNPAID");
        entity.setSubtotal(BigDecimal.ZERO);
        entity.setDiscountTotal(BigDecimal.ZERO);
        entity.setTotalAmount(BigDecimal.ZERO);
        updateDraft(dto, entity);
        return entity;
    }

    public void updateDraft(OrderDto dto, Order entity) {
        entity.setOrderCode(dto.getOrderCode().trim().toUpperCase(Locale.ROOT));
        entity.setCustomerId(dto.getCustomerId());
        entity.setStoreId(dto.getStoreId());
        entity.setOrderType(dto.getOrderType().trim().toUpperCase(Locale.ROOT));
        entity.setTax(dto.getTax() == null ? BigDecimal.ZERO : dto.getTax());
        entity.setShippingFee(dto.getShippingFee() == null ? BigDecimal.ZERO : dto.getShippingFee());
        entity.setNote(dto.getNote());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }
}
