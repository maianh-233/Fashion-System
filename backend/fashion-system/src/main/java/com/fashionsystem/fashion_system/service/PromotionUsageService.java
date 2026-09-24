package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.PromotionUsageDto;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionUsage;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PromotionUsageMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionUsageRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ghi nhận và truy vấn lịch sử sử dụng khuyến mãi theo mô hình append-only. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("PROMOTION")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionUsageService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "promotionId", "orderId", "customerId", "usedAt");
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository usageRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PromotionUsageMapper mapper;

    /** Ghi nhận một lần sử dụng sau khi kiểm tra hiệu lực và giới hạn khuyến mãi. */
    @Transactional
    public PromotionUsageDto recordUsage(UUID promotionId, UUID orderId, UUID customerId) {
        Promotion promotion = promotionRepository.findByIdForUpdate(promotionId)
                .orElseThrow(() -> BusinessException.notFound("Khuyến mãi không tồn tại"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        validateCustomer(order, customerId);
        validateEffectiveAndLimits(promotion, customerId, LocalDateTime.now());
        if (usageRepository.existsByPromotionIdAndOrderId(promotionId, orderId))
            throw BusinessException.conflict("Khuyến mãi đã được ghi nhận cho đơn hàng");
        PromotionUsage usage = PromotionUsage.builder()
                .promotionId(promotionId)
                .orderId(orderId)
                .customerId(customerId)
                .usedAt(LocalDateTime.now())
                .build();
        return mapper.toDto(usageRepository.save(usage));
    }

    /** Lấy chi tiết một lần sử dụng khuyến mãi. */
    @Transactional(readOnly = true)
    public PromotionUsageDto getById(UUID id) {
        return mapper.toDto(usageRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Lịch sử sử dụng khuyến mãi không tồn tại")));
    }

    /** Lấy lịch sử sử dụng khuyến mãi theo bộ lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<PromotionUsageDto> getList(
            UUID promotionId, UUID orderId, UUID customerId,
            LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw BusinessException.badRequest("Khoảng ngày sử dụng không hợp lệ");
        validateSort(pageable);
        return usageRepository.search(promotionId, orderId, customerId, fromDate, toDate, pageable).map(mapper::toDto);
    }

    private void validateCustomer(Order order, UUID customerId) {
        if (customerId != null && !customerRepository.existsById(customerId))
            throw BusinessException.notFound("Khách hàng không tồn tại");
        if (order.getCustomerId() != null && !order.getCustomerId().equals(customerId))
            throw BusinessException.badRequest("Khách hàng không khớp với đơn hàng");
    }
    private void validateEffectiveAndLimits(Promotion promotion, UUID customerId, LocalDateTime now) {
        if (!Boolean.TRUE.equals(promotion.getActive())) throw BusinessException.invalidState("Khuyến mãi chưa được kích hoạt");
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())
                || promotion.getEndDate() != null && now.isAfter(promotion.getEndDate()))
            throw BusinessException.invalidState("Khuyến mãi không nằm trong thời gian hiệu lực");
        if (promotion.getUsageLimit() != null
                && usageRepository.countByPromotionId(promotion.getId()) >= promotion.getUsageLimit())
            throw BusinessException.invalidState("Khuyến mãi đã hết lượt sử dụng");
        if (promotion.getUsagePerUser() != null) {
            if (customerId == null) throw BusinessException.badRequest("Khuyến mãi yêu cầu xác định khách hàng");
            if (usageRepository.countByPromotionIdAndCustomerId(promotion.getId(), customerId)
                    >= promotion.getUsagePerUser())
                throw BusinessException.invalidState("Khách hàng đã hết lượt sử dụng khuyến mãi");
        }
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp lịch sử khuyến mãi không hợp lệ");
    }
}
