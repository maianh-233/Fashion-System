package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CustomerTierMapper;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ cấu hình hạng khách hàng. */
@Service
@RequiredArgsConstructor
public class CustomerTierService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "code", "name", "minTotalSpent", "discountPercent", "createdAt", "updatedAt");

    private final CustomerTierRepository tierRepository;
    private final CustomerTierAssignmentRepository assignmentRepository;
    private final CustomerTierMapper tierMapper;

    /**
     * Tạo mới một hạng khách hàng.
     */
    @Transactional
    public CustomerTierDto create(CustomerTierDto request) {
        ensureCodeAvailable(request.getCode(), null);
        return tierMapper.toDto(tierRepository.save(tierMapper.toEntity(request)));
    }

    /**
     * Lấy chi tiết hạng khách hàng theo ID.
     */
    @Transactional(readOnly = true)
    public CustomerTierDto getById(UUID id) {
        return tierMapper.toDto(requireTier(id));
    }

    /**
     * Lấy danh sách hạng khách hàng với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CustomerTierDto> getList(
            String keyword,
            BigDecimal minSpentFrom,
            BigDecimal minSpentTo,
            BigDecimal discountFrom,
            BigDecimal discountTo,
            Pageable pageable) {
        validateRange(minSpentFrom, minSpentTo, "Khoảng tổng chi tiêu tối thiểu không hợp lệ");
        validateRange(discountFrom, discountTo, "Khoảng phần trăm giảm giá không hợp lệ");
        validateSort(pageable);
        return tierRepository.search(
                        trimToEmpty(keyword), minSpentFrom, minSpentTo,
                        discountFrom, discountTo, pageable)
                .map(tierMapper::toDto);
    }

    /**
     * Cập nhật cấu hình hạng khách hàng.
     */
    @Transactional
    public CustomerTierDto update(UUID id, CustomerTierDto request) {
        CustomerTier entity = requireTier(id);
        ensureCodeAvailable(request.getCode(), id);
        tierMapper.updateEntity(request, entity);
        return tierMapper.toDto(tierRepository.save(entity));
    }

    /**
     * Xóa hạng khách hàng chưa từng được gán.
     */
    @Transactional
    public void delete(UUID id) {
        requireTier(id);
        if (assignmentRepository.existsByTierId(id)) {
            throw BusinessException.invalidState("Không thể xóa hạng khách hàng đã được gán");
        }
        tierRepository.deleteById(id);
    }

    /**
     * Lấy toàn bộ hạng theo thứ tự ngưỡng chi tiêu tăng dần.
     */
    @Transactional(readOnly = true)
    public List<CustomerTierDto> getOrderedTiers() {
        return tierRepository.findAllByOrderByMinTotalSpentAscCodeAsc().stream()
                .map(tierMapper::toDto)
                .toList();
    }

    /**
     * Xác định hạng phù hợp nhất với tổng tiền đã chi.
     */
    @Transactional(readOnly = true)
    public CustomerTierDto preview(BigDecimal totalSpent) {
        if (totalSpent == null || totalSpent.signum() < 0) {
            throw BusinessException.badRequest("Tổng chi tiêu phải lớn hơn hoặc bằng 0");
        }
        return tierMapper.toDto(tierRepository
                .findFirstByMinTotalSpentLessThanEqualOrderByMinTotalSpentDesc(totalSpent)
                .orElseThrow(() -> BusinessException.notFound("Chưa có hạng phù hợp với tổng chi tiêu")));
    }

    private CustomerTier requireTier(UUID id) {
        return tierRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Hạng khách hàng không tồn tại"));
    }

    private void ensureCodeAvailable(String code, UUID excludedId) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        boolean exists = excludedId == null
                ? tierRepository.existsByCode(normalized)
                : tierRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Mã hạng khách hàng đã tồn tại");
    }

    private void validateRange(BigDecimal from, BigDecimal to, String message) {
        if (from != null && to != null && from.compareTo(to) > 0) {
            throw BusinessException.badRequest(message);
        }
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp hạng khách hàng không hợp lệ");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
