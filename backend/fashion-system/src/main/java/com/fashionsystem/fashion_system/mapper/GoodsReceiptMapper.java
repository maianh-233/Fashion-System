package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.GoodsReceiptDto;
import com.fashionsystem.fashion_system.entity.GoodsReceipt;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link GoodsReceipt} và {@link GoodsReceiptDto}.
 */
@Component
public class GoodsReceiptMapper {
    public GoodsReceiptDto toDto(GoodsReceipt entity) {
        if (entity == null) {
            return null;
        }
        GoodsReceiptDto dto = new GoodsReceiptDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public GoodsReceipt toEntity(GoodsReceiptDto dto) {
        if (dto == null) {
            return null;
        }
        GoodsReceipt entity = new GoodsReceipt();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setReceiptDate(dto.getReceiptDate() == null ? LocalDateTime.now() : dto.getReceiptDate());
        entity.setStatus("PENDING");
        entity.setTotalQuantity(0);
        entity.setTotalAmount(BigDecimal.ZERO);
        updateDraft(dto, entity);
        return entity;
    }

    public void updateDraft(GoodsReceiptDto dto, GoodsReceipt entity) {
        entity.setReceiptCode(dto.getReceiptCode().trim().toUpperCase(Locale.ROOT));
        entity.setSupplierId(dto.getSupplierId());
        entity.setStoreId(dto.getStoreId());
        entity.setReceivedBy(dto.getReceivedBy());
        entity.setReceiptDate(dto.getReceiptDate() == null ? entity.getReceiptDate() : dto.getReceiptDate());
        entity.setNote(dto.getNote());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }
}
