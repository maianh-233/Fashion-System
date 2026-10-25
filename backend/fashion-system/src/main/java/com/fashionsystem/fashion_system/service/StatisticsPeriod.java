package com.fashionsystem.fashion_system.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Kỳ báo cáo trên trang thống kê, luôn tính đến hết hôm nay và so với kỳ liền trước. */
public enum StatisticsPeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_QUARTER,
    LAST_12_MONTHS;

    public enum Granularity { DAY, MONTH }

    /**
     * Khoảng ngày nửa mở {@code [from, to)} của kỳ và kỳ so sánh.
     * Quý này so với cùng số ngày đầu quý trước; 12 tháng so với 12 tháng trước đó.
     */
    public record Range(LocalDate from, LocalDate to, LocalDate previousFrom, LocalDate previousTo,
                        Granularity granularity) {
    }

    public Range resolve(LocalDate today) {
        LocalDate to = today.plusDays(1);
        return switch (this) {
            case LAST_7_DAYS -> days(to, 7);
            case LAST_30_DAYS -> days(to, 30);
            case THIS_QUARTER -> {
                LocalDate from = LocalDate.of(today.getYear(), (today.getMonthValue() - 1) / 3 * 3 + 1, 1);
                LocalDate previousFrom = from.minusMonths(3);
                LocalDate previousTo = previousFrom.plusDays(ChronoUnit.DAYS.between(from, to));
                yield new Range(from, to, previousFrom, previousTo.isAfter(from) ? from : previousTo, Granularity.DAY);
            }
            case LAST_12_MONTHS -> {
                LocalDate from = today.withDayOfMonth(1).minusMonths(11);
                yield new Range(from, to, from.minusYears(1), to.minusYears(1), Granularity.MONTH);
            }
        };
    }

    private static Range days(LocalDate to, int length) {
        LocalDate from = to.minusDays(length);
        return new Range(from, to, from.minusDays(length), from, Granularity.DAY);
    }
}
