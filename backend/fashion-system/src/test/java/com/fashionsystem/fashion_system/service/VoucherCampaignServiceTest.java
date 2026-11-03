package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.promotion.VoucherCampaignDtos.Criteria;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherCampaignServiceTest {
    @Test void segmentAddsOnlyTheChosenCriteria() {
        String everyone = VoucherCampaignService.segment(new Criteria(null, null, null, false));
        assertThat(everyone.trim()).isEqualTo("where c.active");

        String all = VoucherCampaignService.segment(new Criteria(List.of(UUID.randomUUID()), 90, UUID.randomUUID(), null));
        assertThat(all).contains("a.tier_id in (:tierIds)")
                .contains("o.status = 'COMPLETED'").contains("o.created_at >= :inactiveSince")
                .contains("p.category_id in (select id from cat_tree)")
                .contains("x.source = 'CAMPAIGN' and x.redeemed_at >= :recentSince")
                .doesNotContain("activeand").startsWith(" where c.active and exists");
        assertThat(all.split("\\s+and (not )?exists")).hasSize(6);
    }

    @Test void skipRecentDefaultsToOnAndTiersAreDeduplicated() {
        UUID gold = UUID.randomUUID();
        Criteria criteria = new Criteria(List.of(gold, gold), null, null, null);
        assertThat(criteria.skipRecentOrDefault()).isTrue();
        assertThat(criteria.tierIdsOrEmpty()).containsExactly(gold);
    }

    @Test void voucherIsUsableThroughTheEndOfTheLastDay() {
        assertThat(VoucherCampaignService.expiresAt(LocalDateTime.of(2026, 11, 2, 21, 30), 7))
                .isEqualTo(LocalDateTime.of(2026, 11, 10, 0, 0));
    }

    @Test void messageCombinesNoteBenefitAndLastDay() {
        var template = new BirthdayVoucherService.Template(UUID.randomUUID(), "KM_ABC123", "Quay lại", "PERCENT",
                BigDecimal.valueOf(15), BigDecimal.valueOf(100_000), BigDecimal.valueOf(300_000));
        var sending = new VoucherCampaignService.Sending(UUID.randomUUID(), "Quay lại", "Lâu rồi không gặp!", template.id(),
                "KM_ABC123", template, 7, new Criteria(null, null, null, null));
        assertThat(VoucherCampaignService.message(sending, LocalDateTime.of(2026, 11, 9, 0, 0)))
                .startsWith("Lâu rồi không gặp! Lunaria tặng bạn voucher KM_ABC123 giảm 15%")
                .contains("tối đa 100.000đ").contains("đơn từ 300.000đ").contains("hết ngày 09/11/2026");
    }
}
