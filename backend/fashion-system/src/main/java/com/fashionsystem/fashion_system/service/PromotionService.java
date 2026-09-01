package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PromotionMapper;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionUsageRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ cấu hình và tính giá trị khuyến mãi. */
@Service
@RequiredArgsConstructor
public class PromotionService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "code", "name", "discountType", "discountValue", "startDate", "endDate",
            "minOrderValue", "usageLimit", "usagePerUser", "active", "createdAt", "updatedAt");
    private final PromotionRepository repository;
    private final PromotionUsageRepository usageRepository;
    private final PromotionMapper mapper;

    /** Tạo mới cấu hình khuyến mãi. */
    @Transactional
    public PromotionDto create(PromotionDto request) {
        validatePromotion(request);
        ensureCodeAvailable(request.getCode(), null);
        return mapper.toDto(repository.save(mapper.toEntity(request)));
    }

    /** Lấy chi tiết khuyến mãi theo ID. */
    @Transactional(readOnly = true)
    public PromotionDto getById(UUID id) { return mapper.toDto(requirePromotion(id)); }

    /** Lấy khuyến mãi với tìm kiếm, lọc hiệu lực và phân trang. */
    @Transactional(readOnly = true)
    public Page<PromotionDto> getList(
            String keyword, String discountType, Boolean active,
            LocalDateTime effectiveAt, Pageable pageable) {
        validateSort(pageable);
        return repository.search(trimToEmpty(keyword), normalize(discountType), active, effectiveAt, pageable)
                .map(mapper::toDto);
    }

    /** Cập nhật cấu hình khuyến mãi. */
    @Transactional
    public PromotionDto update(UUID id, PromotionDto request) {
        Promotion entity = requirePromotion(id);
        validatePromotion(request);
        ensureCodeAvailable(request.getCode(), id);
        mapper.updateEntity(request, entity);
        return mapper.toDto(repository.save(entity));
    }

    /** Xóa khuyến mãi chưa phát sinh lịch sử sử dụng. */
    @Transactional
    public void delete(UUID id) {
        Promotion entity = requirePromotion(id);
        if (usageRepository.existsByPromotionId(id))
            throw BusinessException.invalidState("Không thể xóa khuyến mãi đã được sử dụng");
        repository.delete(entity);
    }

    /** Kích hoạt cấu hình khuyến mãi. */
    @Transactional
    public PromotionDto activate(UUID id) {
        Promotion entity = requirePromotion(id);
        entity.setActive(Boolean.TRUE);
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(entity));
    }

    /** Vô hiệu hóa cấu hình khuyến mãi. */
    @Transactional
    public PromotionDto deactivate(UUID id) {
        Promotion entity = requirePromotion(id);
        entity.setActive(Boolean.FALSE);
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(entity));
    }

    /** Tính trước số tiền giảm theo cấu hình và giá trị đơn hàng. */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(UUID id, BigDecimal orderValue, LocalDateTime effectiveAt) {
        Promotion promotion = requirePromotion(id);
        validateEffective(promotion, effectiveAt == null ? LocalDateTime.now() : effectiveAt);
        if (orderValue == null || orderValue.signum() < 0)
            throw BusinessException.badRequest("Giá trị đơn hàng không hợp lệ");
        if (promotion.getMinOrderValue() != null && orderValue.compareTo(promotion.getMinOrderValue()) < 0)
            return BigDecimal.ZERO;
        BigDecimal discount = "PERCENT".equals(promotion.getDiscountType())
                ? orderValue.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : promotion.getDiscountValue();
        if (promotion.getMaxDiscount() != null) discount = discount.min(promotion.getMaxDiscount());
        return discount.min(orderValue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private Promotion requirePromotion(UUID id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("Khuyến mãi không tồn tại"));
    }
    private void validatePromotion(PromotionDto request) {
        String type = normalize(request.getDiscountType());
        if (!Set.of("PERCENT", "FIXED").contains(type))
            throw BusinessException.badRequest("Loại giảm giá chỉ hỗ trợ PERCENT hoặc FIXED");
        if (request.getDiscountValue() == null || request.getDiscountValue().signum() < 0)
            throw BusinessException.badRequest("Giá trị giảm giá không được âm");
        if ("PERCENT".equals(type) && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            throw BusinessException.badRequest("Phần trăm giảm giá không được vượt quá 100");
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate()))
            throw BusinessException.badRequest("Thời gian khuyến mãi không hợp lệ");
        if (request.getMinOrderValue() != null && request.getMinOrderValue().signum() < 0
                || request.getMaxDiscount() != null && request.getMaxDiscount().signum() < 0)
            throw BusinessException.badRequest("Ngưỡng tiền khuyến mãi không được âm");
        if (request.getUsageLimit() != null && request.getUsageLimit() <= 0
                || request.getUsagePerUser() != null && request.getUsagePerUser() <= 0)
            throw BusinessException.badRequest("Giới hạn sử dụng phải lớn hơn 0");
        if (request.getUsageLimit() != null && request.getUsagePerUser() != null
                && request.getUsagePerUser() > request.getUsageLimit())
            throw BusinessException.badRequest("Giới hạn mỗi khách không được vượt tổng giới hạn");
    }
    private void validateEffective(Promotion promotion, LocalDateTime at) {
        if (!Boolean.TRUE.equals(promotion.getActive())) throw BusinessException.invalidState("Khuyến mãi chưa được kích hoạt");
        if (promotion.getStartDate() != null && at.isBefore(promotion.getStartDate())
                || promotion.getEndDate() != null && at.isAfter(promotion.getEndDate()))
            throw BusinessException.invalidState("Khuyến mãi không nằm trong thời gian hiệu lực");
    }
    private void ensureCodeAvailable(String code, UUID excludedId) {
        String value = code.trim().toUpperCase(Locale.ROOT);
        boolean exists = excludedId == null ? repository.existsByCode(value) : repository.existsByCodeAndIdNot(value, excludedId);
        if (exists) throw BusinessException.conflict("Mã khuyến mãi đã tồn tại");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp khuyến mãi không hợp lệ");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
