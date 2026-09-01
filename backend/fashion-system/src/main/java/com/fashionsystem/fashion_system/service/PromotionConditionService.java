package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.PromotionConditionDto;
import com.fashionsystem.fashion_system.entity.PromotionCondition;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PromotionConditionMapper;
import com.fashionsystem.fashion_system.repository.PromotionConditionRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý các điều kiện cấu hình động thuộc khuyến mãi. */
@Service
@RequiredArgsConstructor
public class PromotionConditionService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "conditionType", "conditionValue", "createdAt");
    private final PromotionRepository promotionRepository;
    private final PromotionConditionRepository conditionRepository;
    private final PromotionConditionMapper mapper;

    /** Thêm điều kiện cấu hình vào khuyến mãi. */
    @Transactional
    public PromotionConditionDto create(UUID promotionId, PromotionConditionDto request) {
        requirePromotion(promotionId);
        PromotionCondition entity = mapper.toEntity(request);
        entity.setPromotionId(promotionId);
        return mapper.toDto(conditionRepository.save(entity));
    }

    /** Lấy chi tiết điều kiện thuộc khuyến mãi. */
    @Transactional(readOnly = true)
    public PromotionConditionDto getById(UUID promotionId, UUID conditionId) {
        requirePromotion(promotionId);
        return mapper.toDto(requireCondition(promotionId, conditionId));
    }

    /** Lấy danh sách điều kiện của khuyến mãi theo phân trang. */
    @Transactional(readOnly = true)
    public Page<PromotionConditionDto> getList(UUID promotionId, Pageable pageable) {
        requirePromotion(promotionId);
        validateSort(pageable);
        return conditionRepository.findAllByPromotionId(promotionId, pageable).map(mapper::toDto);
    }

    /** Cập nhật điều kiện cấu hình thuộc khuyến mãi. */
    @Transactional
    public PromotionConditionDto update(UUID promotionId, UUID conditionId, PromotionConditionDto request) {
        requirePromotion(promotionId);
        PromotionCondition entity = requireCondition(promotionId, conditionId);
        mapper.updateEntity(request, entity);
        return mapper.toDto(conditionRepository.save(entity));
    }

    /** Xóa điều kiện cấu hình khỏi khuyến mãi. */
    @Transactional
    public void delete(UUID promotionId, UUID conditionId) {
        requirePromotion(promotionId);
        conditionRepository.delete(requireCondition(promotionId, conditionId));
    }

    private void requirePromotion(UUID id) {
        if (!promotionRepository.existsById(id)) throw BusinessException.notFound("Khuyến mãi không tồn tại");
    }
    private PromotionCondition requireCondition(UUID promotionId, UUID id) {
        return conditionRepository.findByIdAndPromotionId(id, promotionId)
                .orElseThrow(() -> BusinessException.notFound("Điều kiện khuyến mãi không tồn tại"));
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp điều kiện không hợp lệ");
    }
}
