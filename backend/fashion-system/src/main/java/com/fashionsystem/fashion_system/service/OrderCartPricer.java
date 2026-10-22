package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.VoucherOptionResponse;
import com.fashionsystem.fashion_system.entity.CustomerPromotion;
import com.fashionsystem.fashion_system.entity.OrderPromotion;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.entity.PromotionUsage;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerPromotionRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderPromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionUsageRepository;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.LineInput;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.LineResult;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.ProductPromotion;
import com.fashionsystem.fashion_system.service.OrderVoucherService.CustomerContext;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Phần tính tiền dùng chung cho đơn tại quầy và đơn online: chọn voucher theo quy tắc cộng dồn,
 * tính tiền, xếp hạng voucher gợi ý và ghi nhận lượt dùng voucher khi lưu đơn.
 */
@Component
@RequiredArgsConstructor
public class OrderCartPricer {
    private final OrderVoucherService vouchers;
    private final OrderPricingCalculator calculator;
    private final PromotionRepository promotionRepository;
    private final CustomerRepository customerRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final PromotionUsageRepository usageRepository;
    private final OrderPromotionRepository orderPromotionRepository;

    /**
     * Kết quả tính tiền giỏ hàng.
     *
     * @param applied   khuyến mãi thực sự được áp dụng → tổng tiền giảm
     * @param warnings  vấn đề về voucher chặn tạo đơn
     * @param notices   thông tin không chặn (voucher sản phẩm đã chọn nhưng không được áp dụng)
     */
    public record PricedCart(OrderPricingCalculator.Result pricing, Map<Promotion, BigDecimal> applied,
                             Set<UUID> selectedIds, List<String> warnings, List<String> notices) {
    }

    /** Khách của đơn (null = khách vãng lai), kiểm tra còn hoạt động. */
    public CustomerContext customer(UUID customerId) {
        if (customerId == null) return CustomerContext.walkIn();
        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        if (Boolean.FALSE.equals(customer.getActive()))
            throw BusinessException.invalidState("Khách hàng đã ngừng hoạt động");
        return vouchers.customer(customerId);
    }

    /**
     * @param lockPromotions khóa các khuyến mãi đã chọn trước khi đếm lượt dùng (khi tạo đơn)
     */
    public PricedCart price(List<LineInput> lines, List<String> codes, CustomerContext customer, boolean lockPromotions) {
        List<String> warnings = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        LocalDateTime at = PromotionService.now();
        List<Promotion> selected = selectedPromotions(codes, lockPromotions, warnings);
        OrderVoucherService.Targets targets = vouchers.targets(selected.stream().map(Promotion::getId).toList());
        Promotion orderPromotion = null;
        List<ProductPromotion> productPromotions = new ArrayList<>();
        for (Promotion promotion : selected) {
            String reason = vouchers.ineligibleReason(promotion, customer, targets, at);
            if (reason != null) {
                warnings.add("Voucher " + promotion.getCode() + ": " + reason);
            } else if (promotion.getPromotionType() == PromotionType.PRODUCT) {
                productPromotions.add(new ProductPromotion(promotion, targets.products(promotion.getId())));
            } else if (orderPromotion == null) {
                orderPromotion = promotion;
            } else {
                warnings.add("Chỉ được dùng 1 voucher cho toàn đơn");
            }
        }

        OrderPricingCalculator.Result pricing = calculator.price(lines, productPromotions, orderPromotion);
        if (orderPromotion != null && pricing.orderPromotionShortfall().signum() > 0)
            warnings.add("Voucher " + orderPromotion.getCode() + ": còn thiếu " + vnd(pricing.orderPromotionShortfall())
                    + " để đạt đơn tối thiểu");

        Map<Promotion, BigDecimal> applied = new LinkedHashMap<>();
        for (LineResult line : pricing.lines())
            if (line.promotion() != null) applied.merge(line.promotion(), line.discount(), BigDecimal::add);
        for (ProductPromotion product : productPromotions)
            if (!applied.containsKey(product.promotion()))
                notices.add("Voucher " + product.promotion().getCode() + " không được áp dụng (không có sản phẩm phù hợp "
                        + "hoặc đã có khuyến mãi tốt hơn)");
        if (orderPromotion != null && pricing.orderDiscount().signum() > 0)
            applied.put(orderPromotion, pricing.orderDiscount());

        Set<UUID> selectedIds = selected.stream().map(Promotion::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        return new PricedCart(pricing, applied, selectedIds, warnings, notices);
    }

    /**
     * Voucher cho giỏ hiện tại, sắp xếp theo số tiền giảm thực tế giảm dần. Voucher toàn đơn được tính
     * trên tạm tính sau các khuyến mãi sản phẩm đang chọn; khuyến mãi sản phẩm tính trên các dòng phù hợp.
     */
    public List<VoucherOptionResponse> voucherOptions(PricedCart current, List<LineInput> lines, CustomerContext customer) {
        LocalDateTime at = PromotionService.now();
        List<Promotion> candidates = vouchers.listable(customer, at);
        OrderVoucherService.Targets targets = vouchers.targets(candidates.stream().map(Promotion::getId).toList());
        BigDecimal orderBase = current.pricing().afterProductDiscount();

        List<VoucherOptionResponse> options = new ArrayList<>();
        for (Promotion promotion : candidates) {
            String reason = vouchers.ineligibleReason(promotion, customer, targets, at);
            BigDecimal discount = BigDecimal.ZERO;
            if (promotion.getPromotionType() == PromotionType.PRODUCT) {
                Set<UUID> products = targets.products(promotion.getId());
                discount = lines.stream().filter(line -> products.contains(line.productId()))
                        .map(line -> calculator.lineDiscount(promotion, line)).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (reason == null && discount.signum() == 0) reason = "Không có sản phẩm áp dụng trong giỏ";
            } else {
                BigDecimal shortfall = calculator.shortfall(promotion, orderBase);
                if (reason == null && shortfall.signum() > 0)
                    reason = "Còn thiếu " + vnd(shortfall) + " để đạt đơn tối thiểu";
                if (reason == null) discount = calculator.orderDiscount(promotion, orderBase);
                if (reason == null && discount.signum() == 0) reason = "Giỏ hàng chưa có sản phẩm";
            }
            boolean eligible = reason == null;
            options.add(new VoucherOptionResponse(promotion.getId(), promotion.getCode(), promotion.getName(),
                    promotion.getPromotionType(), promotion.getDiscountType(), promotion.getDiscountValue(),
                    promotion.getMaxDiscount(), promotion.getMinOrderValue(), promotion.getEndDate(),
                    eligible ? discount : BigDecimal.ZERO, eligible, reason, false,
                    current.selectedIds().contains(promotion.getId())));
        }
        options.sort(Comparator.comparing(VoucherOptionResponse::eligible).reversed()
                .thenComparing(VoucherOptionResponse::discount, Comparator.reverseOrder())
                .thenComparing(VoucherOptionResponse::code));
        return markSuggestions(options);
    }

    /**
     * Lưu khuyến mãi đã áp dụng cho đơn và ghi lượt dùng: voucher đổi điểm được đánh dấu đã dùng, voucher
     * thường ghi một dòng promotion_usages. Hủy/hoàn đơn sau này không trả lại lượt dùng.
     */
    public void recordApplied(Map<Promotion, BigDecimal> applied, UUID orderId, UUID customerId, LocalDateTime now) {
        for (Map.Entry<Promotion, BigDecimal> entry : applied.entrySet()) {
            Promotion promotion = entry.getKey();
            orderPromotionRepository.save(OrderPromotion.builder().orderId(orderId).promotionId(promotion.getId())
                    .promotionCode(promotion.getCode()).discountAmount(entry.getValue()).createdAt(now).build());
            if (promotion.getPromotionType() == PromotionType.POINT_REDEMPTION) {
                CustomerPromotion owned = customerPromotionRepository.findUnusedForUpdate(customerId, promotion.getId())
                        .stream().findFirst()
                        .orElseThrow(() -> BusinessException.invalidState(
                                "Voucher " + promotion.getCode() + " của khách đã được sử dụng"));
                owned.setUsedOrderId(orderId);
                owned.setUsedAt(now);
                customerPromotionRepository.save(owned);
            } else {
                usageRepository.save(PromotionUsage.builder().promotionId(promotion.getId()).orderId(orderId)
                        .customerId(customerId).usedAt(now).build());
            }
        }
    }

    private List<Promotion> selectedPromotions(List<String> codes, boolean lock, List<String> warnings) {
        Set<String> normalized = codes.stream().filter(Objects::nonNull).map(c -> c.trim().toUpperCase(Locale.ROOT))
                .filter(c -> !c.isEmpty()).collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) return List.of();
        List<Promotion> found = promotionRepository.findAllByCodeIn(normalized);
        Set<String> foundCodes = found.stream().map(Promotion::getCode).collect(Collectors.toSet());
        normalized.stream().filter(code -> !foundCodes.contains(code))
                .forEach(code -> warnings.add("Không tìm thấy voucher " + code));
        if (!lock) return found;
        // Khóa theo thứ tự id để đếm lượt dùng chính xác khi nhiều đơn cùng dùng một voucher.
        return found.stream().map(Promotion::getId).sorted()
                .map(id -> promotionRepository.findByIdForUpdate(id).orElseThrow())
                .toList();
    }

    /** Đánh dấu voucher giảm nhiều nhất của mỗi nhóm (toàn đơn / sản phẩm) vì hai nhóm dùng chung được. */
    private static List<VoucherOptionResponse> markSuggestions(List<VoucherOptionResponse> sorted) {
        UUID bestOrder = sorted.stream().filter(v -> v.eligible() && v.type() != PromotionType.PRODUCT)
                .findFirst().map(VoucherOptionResponse::id).orElse(null);
        UUID bestProduct = sorted.stream().filter(v -> v.eligible() && v.type() == PromotionType.PRODUCT)
                .findFirst().map(VoucherOptionResponse::id).orElse(null);
        return sorted.stream().map(v -> v.id().equals(bestOrder) || v.id().equals(bestProduct)
                ? new VoucherOptionResponse(v.id(), v.code(), v.name(), v.type(), v.discountType(), v.discountValue(),
                v.maxDiscount(), v.minOrderValue(), v.endDate(), v.discount(), v.eligible(), v.reason(), true,
                v.selected())
                : v).toList();
    }

    public static String vnd(BigDecimal amount) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + "đ";
    }
}
