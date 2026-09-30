package com.fashionsystem.fashion_system.mapper;
import com.fashionsystem.fashion_system.dto.GoodsIssueDto;
import com.fashionsystem.fashion_system.entity.GoodsIssue;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
@Component
public class GoodsIssueMapper {
    public GoodsIssueDto toDto(GoodsIssue entity) {
        if (entity == null) return null;
        var dto = new GoodsIssueDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
