package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.service.StatisticsPeriod.Granularity;
import com.fashionsystem.fashion_system.service.StatisticsPeriod.Range;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StatisticsPeriodTest {
    final LocalDate today = LocalDate.of(2026, 10, 25);

    @Test
    void lastDaysIncludeTodayAndCompareWithTheBlockBefore() {
        assertThat(StatisticsPeriod.LAST_7_DAYS.resolve(today)).isEqualTo(new Range(
                LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19), Granularity.DAY));
        Range month = StatisticsPeriod.LAST_30_DAYS.resolve(today);
        assertThat(month.from()).isEqualTo(LocalDate.of(2026, 9, 26));
        assertThat(month.previousFrom()).isEqualTo(LocalDate.of(2026, 8, 27));
        assertThat(month.previousTo()).isEqualTo(month.from());
    }

    @Test
    void quarterComparesWithTheSameElapsedDaysOfThePreviousQuarter() {
        assertThat(StatisticsPeriod.THIS_QUARTER.resolve(today)).isEqualTo(new Range(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 26), Granularity.DAY));
        Range lateQuarter = StatisticsPeriod.THIS_QUARTER.resolve(LocalDate.of(2026, 3, 31));
        assertThat(lateQuarter.from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(lateQuarter.previousFrom()).isEqualTo(LocalDate.of(2025, 10, 1));
        assertThat(lateQuarter.previousTo()).isEqualTo(LocalDate.of(2025, 12, 30));
    }

    @Test
    void lastTwelveMonthsStartsAtAMonthBoundary() {
        assertThat(StatisticsPeriod.LAST_12_MONTHS.resolve(today)).isEqualTo(new Range(
                LocalDate.of(2025, 11, 1), LocalDate.of(2026, 10, 26),
                LocalDate.of(2024, 11, 1), LocalDate.of(2025, 10, 26), Granularity.MONTH));
    }
}
