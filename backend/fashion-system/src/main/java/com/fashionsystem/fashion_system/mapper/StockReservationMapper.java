package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.StockReservationDto;
import com.fashionsystem.fashion_system.entity.StockReservation;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link StockReservation} và {@link StockReservationDto}.
 */
@Component
public class StockReservationMapper {
    public StockReservationDto toDto(StockReservation entity) {
        if (entity == null) {
            return null;
        }
        StockReservationDto dto = new StockReservationDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public StockReservation toEntity(StockReservationDto dto) {
        if (dto == null) {
            return null;
        }
        StockReservation entity = new StockReservation();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

