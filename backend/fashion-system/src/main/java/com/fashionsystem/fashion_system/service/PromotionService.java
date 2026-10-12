package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheKeys;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.dto.promotion.PromotionRequest;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionProduct;
import com.fashionsystem.fashion_system.entity.PromotionStatus;
import com.fashionsystem.fashion_system.entity.PromotionTier;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PromotionMapper;
import com.fashionsystem.fashion_system.repository.CustomerPromotionRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.PromotionProductRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionTierRepository;
import com.fashionsystem.fashion_system.repository.PromotionUsageRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ cấu hình, vòng đời và tính giá trị khuyến mãi. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("PROMOTION")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionService {
    /** Mọi mốc thời gian khuyến mãi được hiểu theo giờ Việt Nam. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String CODE_PREFIX = "KM_";
    /** Bỏ 0/O và 1/I/L để khách gõ lại mã không bị nhầm. */
    private static final char[] CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 6;
    private static final int CODE_ATTEMPTS = 20;
    private static final Set<String> SORT_FIELDS = Set.of(
            "code", "name", "promotionType", "discountType", "discountValue", "startDate", "endDate",
            "pointsRequired", "status", "createdAt", "updatedAt");

    private final SecureRandom random = new SecureRandom();
    private final PromotionRepository repository;
    private final PromotionUsageRepository usageRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionTierRepository promotionTierRepository;
    private final ProductRepository productRepository;
    private final CustomerTierRepository tierRepository;
    private final PromotionMapper mapper;
    private final CatalogReadCacheService cacheReads;

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /** Tạo khuyến mãi mới với mã do hệ thống sinh. */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PROMOTION_LIST, allEntries = true)
    public PromotionDto create(PromotionRequest request) {
        LocalDateTime now = now();
        validate(request);
        if (!request.endDate().isAfter(now))
            throw BusinessException.badRequest("Ngày kết thúc phải sau thời điểm hiện tại");
        Promotion entity = new Promotion();
        entity.setCode(generateCode());
        entity.setPromotionType(request.promotionType());
        entity.setCreatedAt(now);
        apply(request, entity);
        entity.setStatus(statusByPeriod(entity, now));
        entity = repository.save(entity);
        replaceTargets(entity.getId(), request);
        return toDetail(entity);
    }

    /** Lấy chi tiết khuyến mãi theo ID. */
    @Cacheable(cacheNames = CacheNames.PROMOTION_DETAIL, key = "#id")
    public PromotionDto getById(UUID id) {
        return toDetail(requirePromotion(id));
    }

    /** Tra cứu khuyến mãi theo mã (không phân biệt hoa thường). */
    public PromotionDto getByCode(String code) {
        String value = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return toDetail(repository.findByCode(value)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy khuyến mãi với mã " + value)));
    }

    /** Lấy danh sách khuyến mãi có lọc và phân trang. */
    public Page<PromotionDto> getList(
            String keyword, PromotionType promotionType, PromotionStatus status, boolean includeInactive,
            LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        validateSort(pageable);
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo))
            throw BusinessException.badRequest("Khoảng ngày lọc không hợp lệ");
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        LocalDateTime from = dateFrom == null ? PromotionRepository.SEARCH_FROM : dateFrom.atStartOfDay();
        LocalDateTime to = dateTo == null ? PromotionRepository.SEARCH_TO : dateTo.atTime(LocalTime.MAX);
        return cacheReads.promotions(CacheKeys.of(normalizedKeyword, promotionType, status, includeInactive,
                        from, to, pageable),
                () -> repository.search(normalizedKeyword, promotionType, status, includeInactive, from, to, pageable)
                        .map(mapper::toDto)).toPage();
    }

    /** Cập nhật khuyến mãi; mã và loại khuyến mãi không thể thay đổi. */
    @Transactional
    @Caching(put = @CachePut(cacheNames = CacheNames.PROMOTION_DETAIL, key = "#id"), evict =
            @CacheEvict(cacheNames = CacheNames.PROMOTION_LIST, allEntries = true))
    public PromotionDto update(UUID id, PromotionRequest request) {
        Promotion entity = requirePromotion(id);
        validate(request);
        if (request.promotionType() != entity.getPromotionType())
            throw BusinessException.badRequest("Không thể thay đổi loại khuyến mãi sau khi tạo");
        if (isInUse(id) && (!entity.getDiscountType().equals(request.discountType())
                || entity.getDiscountValue().compareTo(request.discountValue()) != 0
                || !entity.getStartDate().equals(request.startDate())))
            throw BusinessException.invalidState(
                    "Khuyến mãi đã được sử dụng nên không thể đổi kiểu giảm, giá trị giảm và ngày bắt đầu");
        LocalDateTime previousEnd = entity.getEndDate();
        apply(request, entity);
        if (!previousEnd.equals(entity.getEndDate())) entity.setExpiryRemindedAt(null);
        if (entity.getStatus() != PromotionStatus.INACTIVE) entity.setStatus(statusByPeriod(entity, now()));
        entity.setUpdatedAt(now());
        entity = repository.save(entity);
        replaceTargets(id, request);
        return toDetail(entity);
    }

    /** Xóa mềm: chuyển khuyến mãi về INACTIVE, giữ nguyên dữ liệu. */
    @Transactional
    @Caching(put = @CachePut(cacheNames = CacheNames.PROMOTION_DETAIL, key = "#id"), evict =
            @CacheEvict(cacheNames = CacheNames.PROMOTION_LIST, allEntries = true))
    public PromotionDto delete(UUID id) {
        Promotion entity = requirePromotion(id);
        if (entity.getStatus() == PromotionStatus.INACTIVE)
            throw BusinessException.invalidState("Khuyến mãi đã ngừng hoạt động");
        entity.setStatus(PromotionStatus.INACTIVE);
        entity.setUpdatedAt(now());
        return toDetail(repository.save(entity));
    }

    /** Khôi phục khuyến mãi đã xóa mềm; trạng thái được tính lại theo thời gian hiệu lực. */
    @Transactional
    @Caching(put = @CachePut(cacheNames = CacheNames.PROMOTION_DETAIL, key = "#id"), evict =
            @CacheEvict(cacheNames = CacheNames.PROMOTION_LIST, allEntries = true))
    public PromotionDto restore(UUID id) {
        Promotion entity = requirePromotion(id);
        if (entity.getStatus() != PromotionStatus.INACTIVE)
            throw BusinessException.invalidState("Chỉ khôi phục được khuyến mãi đang ngừng hoạt động");
        entity.setStatus(statusByPeriod(entity, now()));
        entity.setUpdatedAt(now());
        return toDetail(repository.save(entity));
    }

    /** Chuyển SCHEDULED sang ACTIVE và ACTIVE/SCHEDULED sang EXPIRED theo thời điểm {@code at}. */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PROMOTION_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PROMOTION_LIST, allEntries = true)})
    public int refreshStatuses(LocalDateTime at) {
        List<Promotion> expired = repository.findAllByStatusInAndEndDateLessThanEqual(
                List.of(PromotionStatus.SCHEDULED, PromotionStatus.ACTIVE), at);
        expired.forEach(p -> transition(p, PromotionStatus.EXPIRED, at));
        List<Promotion> started = repository.findAllByStatusAndStartDateLessThanEqual(PromotionStatus.SCHEDULED, at);
        started.forEach(p -> transition(p, PromotionStatus.ACTIVE, at));
        repository.saveAll(expired);
        repository.saveAll(started);
        return expired.size() + started.size();
    }

    /** Khuyến mãi ACTIVE sẽ hết hạn trong khoảng (at, at + days] và chưa được nhắc. */
    public List<Promotion> findExpiringUnreminded(LocalDateTime at, int days) {
        return repository.findAllByStatusAndEndDateGreaterThanAndEndDateLessThanEqualAndExpiryRemindedAtIsNull(
                PromotionStatus.ACTIVE, at, at.plusDays(days));
    }

    /** Đánh dấu đã nhắc; trả về false khi khuyến mãi đã được nhắc hoặc không còn ACTIVE. */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PROMOTION_DETAIL, key = "#id")
    public boolean markExpiryReminded(UUID id, LocalDateTime at) {
        Promotion promotion = repository.findByIdForUpdate(id).orElse(null);
        if (promotion == null || promotion.getStatus() != PromotionStatus.ACTIVE
                || promotion.getExpiryRemindedAt() != null) return false;
        promotion.setExpiryRemindedAt(at);
        repository.save(promotion);
        return true;
    }

    /** Tính trước số tiền giảm theo cấu hình và giá trị đơn hàng. */
    public BigDecimal calculateDiscount(UUID id, BigDecimal orderValue, LocalDateTime effectiveAt) {
        Promotion promotion = requirePromotion(id);
        validateEffective(promotion, effectiveAt == null ? now() : effectiveAt);
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

    /** Trạng thái đúng theo thời gian hiệu lực, dùng khi tạo, sửa và khôi phục. */
    static PromotionStatus statusByPeriod(Promotion promotion, LocalDateTime at) {
        if (!promotion.getEndDate().isAfter(at)) return PromotionStatus.EXPIRED;
        if (promotion.getStartDate().isAfter(at)) return PromotionStatus.SCHEDULED;
        return PromotionStatus.ACTIVE;
    }

    PromotionDto toDetail(Promotion entity) {
        PromotionDto dto = mapper.toDto(entity);
        List<UUID> productIds = promotionProductRepository.findAllByPromotionId(entity.getId()).stream()
                .map(PromotionProduct::getProductId).toList();
        dto.setProducts(productRepository.findAllById(productIds).stream()
                .sorted(Comparator.comparing(Product::getName, Comparator.nullsLast(String::compareTo)))
                .map(p -> new PromotionDto.Target(p.getId(), p.getCode(), p.getName())).toList());
        List<UUID> tierIds = promotionTierRepository.findAllByPromotionId(entity.getId()).stream()
                .map(PromotionTier::getTierId).toList();
        dto.setTiers(tierRepository.findAllById(tierIds).stream()
                .sorted(Comparator.comparing(CustomerTier::getName, Comparator.nullsLast(String::compareTo)))
                .map(t -> new PromotionDto.Target(t.getId(), t.getCode(), t.getName())).toList());
        long used = usageRepository.countByPromotionId(entity.getId())
                + customerPromotionRepository.countByPromotionId(entity.getId());
        dto.setUsedCount(used);
        dto.setInUse(used > 0);
        return dto;
    }

    private void transition(Promotion promotion, PromotionStatus status, LocalDateTime at) {
        promotion.setStatus(status);
        promotion.setUpdatedAt(at);
    }

    /** Ghi các trường được phép sửa; trường không thuộc loại khuyến mãi bị bỏ trống. */
    private void apply(PromotionRequest request, Promotion entity) {
        PromotionType type = entity.getPromotionType();
        boolean percent = "PERCENT".equals(request.discountType());
        entity.setName(request.name().trim());
        entity.setDiscountType(request.discountType());
        entity.setDiscountValue(request.discountValue());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setMaxDiscount(percent ? request.maxDiscount() : null);
        entity.setMinOrderValue(type == PromotionType.PRODUCT ? null : request.minOrderValue());
        entity.setUsageLimit(request.usageLimit());
        entity.setUsagePerUser(type == PromotionType.POINT_REDEMPTION ? null : request.usagePerUser());
        entity.setPointsRequired(type == PromotionType.POINT_REDEMPTION ? request.pointsRequired() : null);
    }

    private void replaceTargets(UUID promotionId, PromotionRequest request) {
        promotionProductRepository.deleteAllByPromotionId(promotionId);
        promotionTierRepository.deleteAllByPromotionId(promotionId);
        if (request.promotionType() == PromotionType.PRODUCT) {
            promotionProductRepository.saveAll(distinct(request.productIds()).stream()
                    .map(productId -> PromotionProduct.builder().promotionId(promotionId).productId(productId).build())
                    .toList());
        }
        promotionTierRepository.saveAll(distinct(request.tierIds()).stream()
                .map(tierId -> PromotionTier.builder().promotionId(promotionId).tierId(tierId).build())
                .toList());
    }

    private void validate(PromotionRequest request) {
        if (request.name() == null || request.name().isBlank())
            throw BusinessException.badRequest("Tên khuyến mãi không được để trống");
        if (request.promotionType() == null)
            throw BusinessException.badRequest("Loại khuyến mãi không được để trống");
        if (!Set.of("PERCENT", "FIXED").contains(request.discountType()))
            throw BusinessException.badRequest("Kiểu giảm giá chỉ hỗ trợ PERCENT hoặc FIXED");
        if (request.discountValue() == null || request.discountValue().signum() <= 0)
            throw BusinessException.badRequest("Giá trị giảm phải lớn hơn 0");
        if ("PERCENT".equals(request.discountType()) && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            throw BusinessException.badRequest("Phần trăm giảm giá không được vượt quá 100");
        if (request.startDate() == null || request.endDate() == null)
            throw BusinessException.badRequest("Ngày bắt đầu và ngày kết thúc không được để trống");
        if (!request.endDate().isAfter(request.startDate()))
            throw BusinessException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        if (request.minOrderValue() != null && request.minOrderValue().signum() < 0)
            throw BusinessException.badRequest("Đơn tối thiểu không được âm");
        if ("PERCENT".equals(request.discountType()) && request.maxDiscount() != null
                && request.maxDiscount().signum() <= 0)
            throw BusinessException.badRequest("Giảm tối đa phải lớn hơn 0");
        if (request.usageLimit() != null && request.usageLimit() <= 0
                || request.usagePerUser() != null && request.usagePerUser() <= 0)
            throw BusinessException.badRequest("Giới hạn lượt dùng phải lớn hơn 0");
        if (request.promotionType() != PromotionType.POINT_REDEMPTION && request.usageLimit() != null
                && request.usagePerUser() != null && request.usagePerUser() > request.usageLimit())
            throw BusinessException.badRequest("Lượt dùng mỗi khách không được vượt tổng giới hạn");
        if (request.promotionType() == PromotionType.POINT_REDEMPTION
                && (request.pointsRequired() == null || request.pointsRequired() <= 0))
            throw BusinessException.badRequest("Khuyến mãi đổi điểm phải có số điểm cần đổi lớn hơn 0");
        if (request.promotionType() == PromotionType.PRODUCT) {
            Set<UUID> productIds = distinct(request.productIds());
            if (productIds.isEmpty())
                throw BusinessException.badRequest("Khuyến mãi theo sản phẩm phải chọn ít nhất một sản phẩm");
            if (productRepository.findAllById(productIds).size() != productIds.size())
                throw BusinessException.notFound("Có sản phẩm không tồn tại");
        }
        Set<UUID> tierIds = distinct(request.tierIds());
        if (!tierIds.isEmpty() && tierRepository.findAllById(tierIds).size() != tierIds.size())
            throw BusinessException.notFound("Có hạng khách hàng không tồn tại");
    }

    private void validateEffective(Promotion promotion, LocalDateTime at) {
        if (promotion.getStatus() != PromotionStatus.ACTIVE)
            throw BusinessException.invalidState("Khuyến mãi không ở trạng thái hoạt động");
        if (at.isBefore(promotion.getStartDate()) || !at.isBefore(promotion.getEndDate()))
            throw BusinessException.invalidState("Khuyến mãi không nằm trong thời gian hiệu lực");
    }

    private boolean isInUse(UUID id) {
        return usageRepository.existsByPromotionId(id) || customerPromotionRepository.existsByPromotionId(id);
    }

    private String generateCode() {
        for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(CODE_PREFIX);
            for (int i = 0; i < CODE_LENGTH; i++) code.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
            if (!repository.existsByCode(code.toString())) return code.toString();
        }
        throw BusinessException.conflict("Không sinh được mã khuyến mãi, vui lòng thử lại");
    }

    private Promotion requirePromotion(UUID id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("Khuyến mãi không tồn tại"));
    }

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp khuyến mãi không hợp lệ");
    }

    private static Set<UUID> distinct(List<UUID> ids) {
        Set<UUID> values = new LinkedHashSet<>();
        if (ids != null) ids.stream().filter(Objects::nonNull).forEach(values::add);
        return values;
    }
}
