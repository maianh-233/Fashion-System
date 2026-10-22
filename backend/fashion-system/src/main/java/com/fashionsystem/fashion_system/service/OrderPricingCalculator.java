package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Promotion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Tính tiền đơn hàng; backend là nguồn đúng duy nhất cho mọi con số hiển thị và lưu.
 *
 * <p>Quy tắc cộng dồn: mỗi dòng sản phẩm được tối đa 1 khuyến mãi sản phẩm (chọn cái giảm nhiều
 * nhất trong các khuyến mãi đã chọn áp dụng cho sản phẩm đó), và cả đơn được tối đa 1 voucher toàn
 * đơn. Voucher toàn đơn tính trên tạm tính sau khi đã trừ giảm giá theo dòng, kể cả điều kiện đơn
 * tối thiểu. Tiền làm tròn tới đồng.</p>
 *
 * <p>Khuyến mãi sản phẩm tính theo từng đơn vị: PERCENT là phần trăm đơn giá (giới hạn bởi giảm tối
 * đa cho mỗi sản phẩm), FIXED là số tiền trừ trên mỗi sản phẩm; không bao giờ vượt đơn giá.</p>
 */
@Component
public class OrderPricingCalculator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Một dòng giỏ hàng với đơn giá đã chốt (giá bán hiện tại của biến thể). */
    public record LineInput(UUID variantId, UUID productId, BigDecimal unitPrice, int quantity) {
    }

    /** Khuyến mãi sản phẩm đã chọn cùng danh sách sản phẩm được áp dụng. */
    public record ProductPromotion(Promotion promotion, Set<UUID> productIds) {
    }

    public record LineResult(LineInput input, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                             Promotion promotion) {
    }

    /**
     * @param orderPromotionShortfall số tiền còn thiếu để đạt đơn tối thiểu của voucher toàn đơn; 0 khi đã đạt
     */
    public record Result(List<LineResult> lines, BigDecimal subtotal, BigDecimal productDiscount,
                         Promotion orderPromotion, BigDecimal orderDiscount, BigDecimal orderPromotionShortfall,
                         BigDecimal total) {
        public BigDecimal discountTotal() {
            return productDiscount.add(orderDiscount);
        }

        /** Tạm tính sau giảm giá theo dòng: cơ sở để tính voucher toàn đơn. */
        public BigDecimal afterProductDiscount() {
            return subtotal.subtract(productDiscount);
        }
    }

    public Result price(List<LineInput> lines, List<ProductPromotion> productPromotions, Promotion orderPromotion) {
        List<LineResult> priced = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal productDiscount = BigDecimal.ZERO;
        for (LineInput line : lines) {
            BigDecimal lineSubtotal = money(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
            ProductPromotion best = bestFor(line, productPromotions);
            BigDecimal discount = best == null ? BigDecimal.ZERO : lineDiscount(best.promotion(), line);
            priced.add(new LineResult(line, lineSubtotal, discount, lineSubtotal.subtract(discount),
                    discount.signum() > 0 ? best.promotion() : null));
            subtotal = subtotal.add(lineSubtotal);
            productDiscount = productDiscount.add(discount);
        }
        BigDecimal base = subtotal.subtract(productDiscount);
        BigDecimal orderDiscount = BigDecimal.ZERO;
        BigDecimal shortfall = BigDecimal.ZERO;
        if (orderPromotion != null) {
            shortfall = shortfall(orderPromotion, base);
            orderDiscount = orderDiscount(orderPromotion, base);
        }
        return new Result(List.copyOf(priced), subtotal, productDiscount, orderPromotion, orderDiscount, shortfall,
                base.subtract(orderDiscount));
    }

    /** Số tiền giảm của khuyến mãi sản phẩm trên cả dòng (0 nếu sản phẩm không thuộc khuyến mãi). */
    public BigDecimal lineDiscount(Promotion promotion, LineInput line) {
        return money(unitDiscount(promotion, line.unitPrice()).multiply(BigDecimal.valueOf(line.quantity())));
    }

    public BigDecimal unitDiscount(Promotion promotion, BigDecimal unitPrice) {
        BigDecimal discount = isPercent(promotion)
                ? unitPrice.multiply(promotion.getDiscountValue()).divide(HUNDRED, 0, RoundingMode.HALF_UP)
                : promotion.getDiscountValue();
        if (isPercent(promotion) && promotion.getMaxDiscount() != null)
            discount = discount.min(promotion.getMaxDiscount());
        return money(discount.min(unitPrice).max(BigDecimal.ZERO));
    }

    /** Giảm giá của voucher toàn đơn trên {@code base}; 0 khi chưa đạt đơn tối thiểu. */
    public BigDecimal orderDiscount(Promotion promotion, BigDecimal base) {
        if (base.signum() <= 0 || shortfall(promotion, base).signum() > 0) return BigDecimal.ZERO;
        BigDecimal discount = isPercent(promotion)
                ? base.multiply(promotion.getDiscountValue()).divide(HUNDRED, 0, RoundingMode.HALF_UP)
                : promotion.getDiscountValue();
        if (isPercent(promotion) && promotion.getMaxDiscount() != null)
            discount = discount.min(promotion.getMaxDiscount());
        return money(discount.min(base).max(BigDecimal.ZERO));
    }

    public BigDecimal shortfall(Promotion promotion, BigDecimal base) {
        BigDecimal min = promotion.getMinOrderValue();
        return min == null || base.compareTo(min) >= 0 ? BigDecimal.ZERO : money(min.subtract(base));
    }

    private ProductPromotion bestFor(LineInput line, List<ProductPromotion> promotions) {
        return promotions.stream()
                .filter(p -> p.productIds().contains(line.productId()))
                .max(Comparator.<ProductPromotion, BigDecimal>comparing(p -> lineDiscount(p.promotion(), line))
                        // Hòa tiền: ưu tiên mã nhỏ hơn để kết quả luôn ổn định giữa quote và tạo đơn.
                        .thenComparing(p -> p.promotion().getCode(), Comparator.reverseOrder()))
                .orElse(null);
    }

    private static boolean isPercent(Promotion promotion) {
        return "PERCENT".equals(promotion.getDiscountType());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
    }
}
