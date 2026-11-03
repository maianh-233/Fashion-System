package com.fashionsystem.fashion_system.service;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoyaltyExpiryServiceTest {
    @Test void pointsAreUsableThroughTheSameDayTwelveMonthsLater() {
        assertThat(LoyaltyPointLots.expiryFor(LocalDateTime.of(2026, 11, 3, 15, 20), 12))
                .isEqualTo(LocalDateTime.of(2027, 11, 4, 0, 0));
        // 29/02 + 12 tháng → 28/02, dùng tới hết ngày đó.
        assertThat(LoyaltyPointLots.expiryFor(LocalDateTime.of(2028, 2, 29, 8, 0), 12))
                .isEqualTo(LocalDateTime.of(2029, 3, 1, 0, 0));
    }

    @Test void reminderNamesThePointsAndTheLastUsableDay() {
        assertThat(LoyaltyExpiryService.reminderMessage(1200, LocalDateTime.of(2027, 11, 4, 0, 0)))
                .isEqualTo("Bạn có 1.200 điểm sẽ hết hạn sau ngày 03/11/2027. Đổi điểm lấy voucher trước khi hết hạn nhé!");
    }

    @Test void expireNoteMentionsAShortBalance() {
        assertThat(LoyaltyExpiryService.expireNote(300, LocalDateTime.of(2025, 11, 3, 10, 0), 300))
                .isEqualTo("Hết hạn 300 điểm tích đến ngày 03/11/2025");
        assertThat(LoyaltyExpiryService.expireNote(8302, null, 120)).isEqualTo("Hết hạn 8.302 điểm (số dư chỉ còn 120 điểm)");
    }
}
