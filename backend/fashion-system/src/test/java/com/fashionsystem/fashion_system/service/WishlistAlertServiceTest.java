package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.service.WishlistAlertService.EventType;
import com.fashionsystem.fashion_system.service.WishlistAlertService.Watched;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WishlistAlertServiceTest {
    final UUID customer = UUID.randomUUID(), product = UUID.randomUUID(), sale = UUID.randomUUID();
    final LocalDateTime now = LocalDateTime.of(2026, 11, 2, 20, 0);

    Watched watched(Long baseline, UUID notifiedSale, LocalDateTime lowAt, Long price, long stock, UUID flash, Long flashPrice) {
        return new Watched(customer, product, "Áo sơ mi", baseline == null ? null : BigDecimal.valueOf(baseline), notifiedSale,
                lowAt, price == null ? null : BigDecimal.valueOf(price), stock, flash,
                flashPrice == null ? null : BigDecimal.valueOf(flashPrice), flash == null ? null : now.plusHours(3));
    }

    List<EventType> types(WishlistAlertService.Decision decision) {
        return decision.events().stream().map(WishlistAlertService.Event::type).toList();
    }

    @Test void firstScanOnlyRecordsTheBaselinePrice() {
        var decision = WishlistAlertService.decide(watched(null, null, null, 500_000L, 50, null, null), now);
        assertThat(decision.events()).isEmpty();
        assertThat(decision.baselinePrice()).isEqualByComparingTo("500000");
    }

    @Test void dropOfAtLeastFivePercentIsReportedAndBecomesTheNewBaseline() {
        var decision = WishlistAlertService.decide(watched(500_000L, null, null, 450_000L, 50, null, null), now);
        assertThat(types(decision)).containsExactly(EventType.PRICE_DROP);
        assertThat(decision.events().getFirst().message()).contains("450.000đ").contains("500.000đ").contains("−10%");
        assertThat(decision.baselinePrice()).isEqualByComparingTo("450000");
    }

    @Test void smallDropIsIgnoredAndRiseRaisesTheBaseline() {
        var small = WishlistAlertService.decide(watched(500_000L, null, null, 480_000L, 50, null, null), now);
        assertThat(small.events()).isEmpty();
        assertThat(small.baselinePrice()).isEqualByComparingTo("500000");
        var up = WishlistAlertService.decide(watched(500_000L, null, null, 600_000L, 50, null, null), now);
        assertThat(up.events()).isEmpty();
        assertThat(up.baselinePrice()).isEqualByComparingTo("600000");
    }

    @Test void flashSaleIsReportedOncePerSaleAndFirst() {
        var first = WishlistAlertService.decide(watched(500_000L, null, null, 450_000L, 3, sale, 299_000L), now);
        assertThat(types(first)).containsExactly(EventType.FLASH_SALE, EventType.PRICE_DROP, EventType.LOW_STOCK);
        assertThat(first.notifiedFlashSaleId()).isEqualTo(sale);
        assertThat(first.events().getFirst().message()).contains("299.000đ").contains("23:00 02/11");

        var again = WishlistAlertService.decide(watched(450_000L, sale, now, 450_000L, 3, sale, 299_000L), now);
        assertThat(again.changes(watched(450_000L, sale, now, 450_000L, 3, sale, 299_000L))).isFalse();
    }

    @Test void lowStockIsReportedOnceAndResetWhenStockRecovers() {
        var low = WishlistAlertService.decide(watched(500_000L, null, null, 500_000L, 5, null, null), now);
        assertThat(types(low)).containsExactly(EventType.LOW_STOCK);
        assertThat(low.lowStockNotifiedAt()).isEqualTo(now);

        var recovered = WishlistAlertService.decide(watched(500_000L, null, now, 500_000L, 6, null, null), now);
        assertThat(recovered.events()).isEmpty();
        assertThat(recovered.lowStockNotifiedAt()).isNull();

        var soldOut = WishlistAlertService.decide(watched(500_000L, null, null, null, 0, null, null), now);
        assertThat(soldOut.events()).isEmpty();
        assertThat(soldOut.baselinePrice()).isEqualByComparingTo("500000");
    }

    @Test void emailBodyListsEveryLineAndHowToTurnItOff() {
        String body = WishlistAlertService.body("Minh Anh", List.of("A giảm còn 1đ.", "B chỉ còn 2 sản phẩm."));
        assertThat(body).contains("Chào Minh Anh").contains("• A giảm còn 1đ.").contains("• B chỉ còn 2 sản phẩm.")
                .contains("Hồ sơ");
        assertThat(WishlistAlertService.subject(false)).contains("sắp hết hàng");
    }
}
