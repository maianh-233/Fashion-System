package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.GoodsIssueDto;
import com.fashionsystem.fashion_system.entity.GoodsIssue;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link GoodsIssue} và {@link GoodsIssueDto}.
 */
@Component
public class GoodsIssueMapper {
    public GoodsIssueDto toDto(GoodsIssue entity) {
        if (entity == null) {
            return null;
        }
        GoodsIssueDto dto = new GoodsIssueDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public GoodsIssue toEntity(GoodsIssueDto dto) {
        if (dto == null) {
            return null;
        }
        GoodsIssue entity = new GoodsIssue();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIssueDate(dto.getIssueDate() == null ? LocalDateTime.now() : dto.getIssueDate());
        entity.setStatus("PENDING");
        entity.setTotalQuantity(0);
        updateDraft(dto, entity);
        return entity;
    }

    public void updateDraft(GoodsIssueDto dto, GoodsIssue entity) {
        entity.setIssueCode(dto.getIssueCode().trim().toUpperCase(Locale.ROOT));
        entity.setStoreId(dto.getStoreId());
        entity.setOrderId(dto.getOrderId());
        entity.setIssuedBy(dto.getIssuedBy());
        entity.setIssueType(dto.getIssueType().trim().toUpperCase(Locale.ROOT));
        entity.setIssueDate(dto.getIssueDate() == null ? entity.getIssueDate() : dto.getIssueDate());
        entity.setNote(dto.getNote());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }
}
