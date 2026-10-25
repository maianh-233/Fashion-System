package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.config.CacheKeys;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CustomerTierMapper;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ cấu hình hạng khách hàng. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("CUSTOMER")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerTierService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "code", "name", "minTotalSpent", "discountPercent", "createdAt", "updatedAt");
    /** Hạng mặc định gán khi tạo khách; luôn tồn tại với ngưỡng 0 để mọi khách đều có hạng. */
    static final String DEFAULT_TIER = "REGULAR";

    private final CustomerTierRepository tierRepository;
    private final CustomerTierAssignmentRepository assignmentRepository;
    private final CustomerTierMapper tierMapper;
    private final CatalogReadCacheService cacheReads;
    private final CustomerAccessService customerAccess;

    /**
     * Tạo mới một hạng khách hàng.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.CUSTOMER_TIER_ORDERED_LIST,
            CacheNames.CUSTOMER_TIER_LIST}, allEntries = true)
    public CustomerTierDto create(UUID actor, CustomerTierDto request) {
        customerAccess.requireTierManager(actor);
        ensureCodeAvailable(request.getCode(), null);
        ensureThresholdAvailable(request.getMinTotalSpent(), null);
        return tierMapper.toDto(tierRepository.save(tierMapper.toEntity(request)));
    }

    /**
     * Lấy chi tiết hạng khách hàng theo ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.CUSTOMER_TIER_DETAIL, key = "#id")
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
        String normalizedKeyword = trimToEmpty(keyword);
        return cacheReads.customerTiers(CacheKeys.of(normalizedKeyword, minSpentFrom, minSpentTo,
                        discountFrom, discountTo, pageable),
                () -> tierRepository.search(normalizedKeyword, minSpentFrom, minSpentTo,
                                discountFrom, discountTo, pageable)
                        .map(tierMapper::toDto)).toPage();
    }

    /**
     * Cập nhật cấu hình hạng khách hàng.
     */
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = CacheNames.CUSTOMER_TIER_DETAIL, key = "#id"),
            evict = @CacheEvict(cacheNames = {CacheNames.CUSTOMER_TIER_ORDERED_LIST,
                    CacheNames.CUSTOMER_TIER_LIST}, allEntries = true))
    public CustomerTierDto update(UUID actor, UUID id, CustomerTierDto request) {
        customerAccess.requireTierManager(actor);
        CustomerTier entity = requireTier(id);
        ensureCodeAvailable(request.getCode(), id);
        ensureThresholdAvailable(request.getMinTotalSpent(), id);
        if (DEFAULT_TIER.equals(entity.getCode())) {
            if (!DEFAULT_TIER.equalsIgnoreCase(request.getCode().trim())) {
                throw BusinessException.badRequest("Không thể đổi mã hạng mặc định " + DEFAULT_TIER);
            }
            if (request.getMinTotalSpent() != null && request.getMinTotalSpent().signum() != 0) {
                throw BusinessException.badRequest("Hạng mặc định " + DEFAULT_TIER + " phải có ngưỡng chi tiêu 0");
            }
        }
        tierMapper.updateEntity(request, entity);
        return tierMapper.toDto(tierRepository.save(entity));
    }

    /**
     * Xóa hạng khách hàng chưa từng được gán.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CUSTOMER_TIER_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = {CacheNames.CUSTOMER_TIER_ORDERED_LIST,
                    CacheNames.CUSTOMER_TIER_LIST}, allEntries = true)
    })
    public void delete(UUID actor, UUID id) {
        customerAccess.requireTierManager(actor);
        if (DEFAULT_TIER.equals(requireTier(id).getCode())) {
            throw BusinessException.invalidState("Không thể xóa hạng mặc định " + DEFAULT_TIER);
        }
        if (assignmentRepository.existsByTierId(id)) {
            throw BusinessException.invalidState("Không thể xóa hạng khách hàng đã được gán");
        }
        tierRepository.deleteById(id);
    }

    /**
     * Lấy toàn bộ hạng theo thứ tự ngưỡng chi tiêu tăng dần.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.CUSTOMER_TIER_ORDERED_LIST, key = "'all'")
    public List<CustomerTierDto> getOrderedTiers() {
        return new ArrayList<>(tierRepository.findAllByOrderByMinTotalSpentAscCodeAsc().stream()
                .map(tierMapper::toDto)
                .toList());
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

    /** Hai hạng cùng ngưỡng khiến việc tự nâng hạng theo tổng chi tiêu không xác định được hạng nào. */
    private void ensureThresholdAvailable(BigDecimal minTotalSpent, UUID excludedId) {
        if (minTotalSpent == null) return;
        boolean exists = excludedId == null
                ? tierRepository.existsByMinTotalSpent(minTotalSpent)
                : tierRepository.existsByMinTotalSpentAndIdNot(minTotalSpent, excludedId);
        if (exists) throw BusinessException.conflict("Đã có hạng khác dùng ngưỡng chi tiêu này");
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
