package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.LineInput;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.ProductPromotion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPricingCalculatorTest {
    final OrderPricingCalculator calculator = new OrderPricingCalculator();
    final UUID shirt = UUID.randomUUID(), jeans = UUID.randomUUID();

    static BigDecimal vnd(long value) {
        return BigDecimal.valueOf(value).setScale(2);
    }

    static Promotion promo(String code, PromotionType type, String discountType, long value, Long max, Long min) {
        return Promotion.builder().id(UUID.randomUUID()).code(code).promotionType(type).discountType(discountType)
                .discountValue(BigDecimal.valueOf(value)).maxDiscount(max == null ? null : BigDecimal.valueOf(max))
                .minOrderValue(min == null ? null : BigDecimal.valueOf(min)).build();
    }

    LineInput line(UUID product, long price, int quantity) {
        return new LineInput(UUID.randomUUID(), product, BigDecimal.valueOf(price), quantity);
    }

    @Test void withoutPromotionsTotalEqualsSubtotal() {
        var result = calculator.price(List.of(line(shirt, 250_000, 2), line(jeans, 499_000, 1)), List.of(), null);
        assertThat(result.subtotal()).isEqualByComparingTo(vnd(999_000));
        assertThat(result.discountTotal()).isEqualByComparingTo(vnd(0));
        assertThat(result.total()).isEqualByComparingTo(vnd(999_000));
    }

    @Test void productPercentDiscountIsPerUnitAndCappedPerUnit() {
        // 20% của 800.000 = 160.000 nhưng giảm tối đa 120.000 cho mỗi sản phẩm.
        var promo = promo("KM_P20", PromotionType.PRODUCT, "PERCENT", 20, 120_000L, null);
        var result = calculator.price(List.of(line(shirt, 800_000, 2)),
                List.of(new ProductPromotion(promo, Set.of(shirt))), null);
        assertThat(result.lines().getFirst().discount()).isEqualByComparingTo(vnd(240_000));
        assertThat(result.lines().getFirst().total()).isEqualByComparingTo(vnd(1_360_000));
        assertThat(result.lines().getFirst().promotion()).isSameAs(promo);
    }

    @Test void flashUnitsSkipProductPromotionButTheRestStillGetIt() {
        // 3 áo 300.000: 2 món giá flash (giảm 100.000/món), món thứ 3 nhận KM sản phẩm 10%; voucher đơn tính sau cùng.
        var promo = promo("KM_P10", PromotionType.PRODUCT, "PERCENT", 10, null, null);
        var order = promo("DON_50K", PromotionType.ORDER, "FIXED", 50_000, null, null);
        LineInput shirts = line(shirt, 300_000, 3);
        var result = calculator.price(List.of(shirts), List.of(new ProductPromotion(promo, Set.of(shirt))), order,
                java.util.Map.of(shirts.variantId(), new OrderPricingCalculator.FlashLine(2, BigDecimal.valueOf(100_000))));
        var priced = result.lines().getFirst();
        assertThat(priced.flashDiscount()).isEqualByComparingTo(vnd(200_000));
        assertThat(priced.promotionDiscount()).isEqualByComparingTo(vnd(30_000));
        assertThat(priced.promotion()).isSameAs(promo);
        assertThat(result.orderDiscount()).isEqualByComparingTo(vnd(50_000));
        assertThat(result.total()).isEqualByComparingTo(vnd(620_000));
    }

    @Test void flashCoveringTheWholeLineLeavesNoPromotion() {
        var promo = promo("KM_P10", PromotionType.PRODUCT, "PERCENT", 10, null, null);
        LineInput shirts = line(shirt, 300_000, 2);
        var priced = calculator.price(List.of(shirts), List.of(new ProductPromotion(promo, Set.of(shirt))), null,
                java.util.Map.of(shirts.variantId(), new OrderPricingCalculator.FlashLine(5, BigDecimal.valueOf(100_000))))
                .lines().getFirst();
        assertThat(priced.flashDiscount()).isEqualByComparingTo(vnd(200_000));
        assertThat(priced.promotion()).isNull();
    }

    @Test void productFixedDiscountNeverExceedsUnitPrice() {
        var promo = promo("KM_F70", PromotionType.PRODUCT, "FIXED", 70_000, null, null);
        var result = calculator.price(List.of(line(shirt, 50_000, 3)),
                List.of(new ProductPromotion(promo, Set.of(shirt))), null);
        assertThat(result.lines().getFirst().discount()).isEqualByComparingTo(vnd(150_000));
        assertThat(result.total()).isEqualByComparingTo(vnd(0));
    }

    @Test void eachLineUsesOnlyTheBestApplicableProductPromotion() {
        var small = promo("KM_A", PromotionType.PRODUCT, "FIXED", 30_000, null, null);
        var big = promo("KM_B", PromotionType.PRODUCT, "PERCENT", 10, null, null);
        var result = calculator.price(List.of(line(shirt, 500_000, 1), line(jeans, 200_000, 1)),
                List.of(new ProductPromotion(small, Set.of(shirt, jeans)), new ProductPromotion(big, Set.of(shirt))), null);
        // Áo: 10% = 50.000 tốt hơn 30.000. Quần: chỉ khuyến mãi A áp dụng.
        assertThat(result.lines().get(0).promotion()).isSameAs(big);
        assertThat(result.lines().get(0).discount()).isEqualByComparingTo(vnd(50_000));
        assertThat(result.lines().get(1).promotion()).isSameAs(small);
        assertThat(result.productDiscount()).isEqualByComparingTo(vnd(80_000));
    }

    @Test void orderVoucherAppliesOnAmountAfterLineDiscountsIncludingMinimumOrder() {
        var product = promo("KM_P", PromotionType.PRODUCT, "FIXED", 50_000, null, null);
        var order = promo("KM_O", PromotionType.ORDER, "FIXED", 50_000, null, 499_000L);
        // Tạm tính 520.000 nhưng sau giảm dòng còn 470.000 < 499.000: voucher toàn đơn chưa dùng được.
        var result = calculator.price(List.of(line(shirt, 520_000, 1)),
                List.of(new ProductPromotion(product, Set.of(shirt))), order);
        assertThat(result.orderDiscount()).isEqualByComparingTo(vnd(0));
        assertThat(result.orderPromotionShortfall()).isEqualByComparingTo(vnd(29_000));
        assertThat(result.total()).isEqualByComparingTo(vnd(470_000));
    }

    @Test void percentOrderVoucherIsCappedAndRoundedToDong() {
        var order = promo("KM_O10", PromotionType.ORDER, "PERCENT", 10, 100_000L, 300_000L);
        var capped = calculator.price(List.of(line(shirt, 1_500_000, 1)), List.of(), order);
        assertThat(capped.orderDiscount()).isEqualByComparingTo(vnd(100_000));

        var rounded = calculator.price(List.of(line(shirt, 333_335, 1)), List.of(), order);
        // 10% của 333.335 = 33.333,5 → làm tròn 33.334.
        assertThat(rounded.orderDiscount()).isEqualByComparingTo(vnd(33_334));
        assertThat(rounded.total()).isEqualByComparingTo(vnd(300_001));
    }
}
