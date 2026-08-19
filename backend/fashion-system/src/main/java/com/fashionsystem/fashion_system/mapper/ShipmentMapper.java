package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ShipmentDto;
import com.fashionsystem.fashion_system.entity.Shipment;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Shipment} và {@link ShipmentDto}.
 */
@Component
public class ShipmentMapper {
    public ShipmentDto toDto(Shipment entity) {
        if (entity == null) {
            return null;
        }
        ShipmentDto dto = new ShipmentDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Shipment toEntity(ShipmentDto dto) {
        if (dto == null) {
            return null;
        }
        Shipment entity = new Shipment();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

